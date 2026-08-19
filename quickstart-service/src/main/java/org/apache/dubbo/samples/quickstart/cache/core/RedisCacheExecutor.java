package org.apache.dubbo.samples.quickstart.cache.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dubbo.samples.quickstart.cache.config.SmartCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 对 Redis 底层操作的集中封装。
 * <p>
 * 负责字符串读写、JSON 序列化/反序列化、空值标记、TTL 抖动以及分布式互斥锁。
 * 除反序列化外，Redis 操作异常会被记录并吞掉，让上层切面能够继续访问 DB，避免 Redis
 * 故障直接中断核心业务。
 */
@Component
public class RedisCacheExecutor {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheExecutor.class);
    /**
     * 用特殊字符串表示“DB 中不存在”，从而区别于 Redis Key 本身不存在。
     */
    private static final String NULL_MARKER = "__SMART_CACHE_NULL__";
    /**
     * Lua 安全解锁机制
     * 仅当锁中的 owner 仍等于当前请求 owner 时才删除锁，防止误删其他请求后来获得的锁。
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final SmartCacheProperties properties;

    public RedisCacheExecutor(StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper,
                              SmartCacheProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    /**
     * 查询 Redis，并转换成明确的 {@link CacheLookup} 状态。
     * Redis 异常返回 ERROR，而不是抛给业务层，上层收到 ERROR 后会降级查询 DB。
     */
    public CacheLookup get(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return CacheLookup.of(CacheLookup.Status.MISS);
            }
            if (NULL_MARKER.equals(value)) {
                return CacheLookup.of(CacheLookup.Status.NULL_VALUE);
            }
            return CacheLookup.hit(value);
        } catch (RuntimeException ex) {
            log.warn("Redis read failed, degrading to database, key={}", key, ex);
            return CacheLookup.of(CacheLookup.Status.ERROR);
        }
    }

    /**
     * 根据业务方法的真实返回类型把缓存 JSON 还原为 Java 对象。
     */
    public Object deserialize(String value, JavaType type) throws JsonProcessingException {
        return objectMapper.readValue(value, type);
    }

    /**
     * 把正常业务对象序列化后写入 Redis。
     * 最终 TTL = 固定 TTL + [0, 随机 TTL 上限]，从而分散缓存集中失效的时间点。
     */
    public void put(String key, Object value, long ttlSeconds, long randomTtlSeconds) {
        try {
            long ttl = resolveTtl(ttlSeconds, randomTtlSeconds);
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), Duration.ofSeconds(ttl));
            log.info("redis put key:{} value:{} ttlSeconds:{} randomTtlSeconds:{}", key, value, ttlSeconds, randomTtlSeconds);
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Redis write failed, key={}", key, ex);
        }
    }

    /**
     * 写入短期空值标记，阻止不存在的数据被反复查询，缓解缓存穿透。
     */
    public void putNull(String key, long nullTtlSeconds) {
        try {
            long ttl = nullTtlSeconds > 0 ? nullTtlSeconds : properties.getNullTtl().toSeconds();
            redisTemplate.opsForValue().set(key, NULL_MARKER, Duration.ofSeconds(Math.max(1, ttl)));
        } catch (RuntimeException ex) {
            log.warn("Redis null-value write failed, key={}", key, ex);
        }
    }

    /**
     * 使用 Redis SET NX 尝试获取缓存重建锁。
     * owner 是每个请求唯一的标识；锁附带 TTL，避免实例宕机造成永久死锁。
     */
    public boolean tryLock(String lockKey, String owner) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue()
                    .setIfAbsent(lockKey, owner, properties.getLockTtl()));
        } catch (RuntimeException ex) {
            log.warn("Redis lock failed, degrading to database, key={}", lockKey, ex);
            return false;
        }
    }

    /**
     * 使用 Lua 原子校验 owner 并释放锁，确保不会释放其他请求持有的锁。
     */
    public void unlock(String lockKey, String owner) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), owner);
        } catch (RuntimeException ex) {
            log.warn("Redis unlock failed, key={}", lockKey, ex);
        }
    }

    /**
     * 删除指定业务缓存；Redis 异常只记录日志，不影响已完成的 DB 操作。
     */
    public void evict(String key) {
        try {
            redisTemplate.delete(key);
        } catch (RuntimeException ex) {
            log.warn("Redis eviction failed, key={}", key, ex);
        }
    }

    /**
     * 根据注解值和全局默认值计算带随机抖动的最终 TTL，且保证至少为 1 秒。
     */
    private long resolveTtl(long configuredTtl, long configuredRandomTtl) {
        long base = configuredTtl > 0 ? configuredTtl : properties.getDefaultTtl().toSeconds();
        long randomBound = configuredRandomTtl >= 0
                ? configuredRandomTtl : properties.getRandomTtl().toSeconds();
        long jitter = randomBound > 0 ? ThreadLocalRandom.current().nextLong(randomBound + 1) : 0;
        return Math.max(1, base + jitter);
    }
}
