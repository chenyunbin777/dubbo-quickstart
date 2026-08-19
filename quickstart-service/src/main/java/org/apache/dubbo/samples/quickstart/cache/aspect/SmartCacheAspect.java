package org.apache.dubbo.samples.quickstart.cache.aspect;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dubbo.samples.quickstart.cache.annotation.SmartCacheEvict;
import org.apache.dubbo.samples.quickstart.cache.annotation.SmartCachePut;
import org.apache.dubbo.samples.quickstart.cache.annotation.SmartCacheable;
import org.apache.dubbo.samples.quickstart.cache.config.SmartCacheProperties;
import org.apache.dubbo.samples.quickstart.cache.core.CacheKeyGenerator;
import org.apache.dubbo.samples.quickstart.cache.core.CacheLookup;
import org.apache.dubbo.samples.quickstart.cache.core.RedisCacheExecutor;
import org.apache.dubbo.samples.quickstart.cache.support.AfterCommitExecutor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 智能缓存注解的核心 AOP 切面。
 * <p>
 * 本类把缓存策略从业务代码中抽离：
 * <ul>
 *   <li>{@link SmartCacheable}：先读缓存，未命中时通过互斥锁控制 DB 回源并重建缓存；</li>
 *   <li>{@link SmartCachePut}：业务更新成功后，在事务提交后写入最新结果；</li>
 *   <li>{@link SmartCacheEvict}：业务删除成功后，在事务提交后删除缓存。</li>
 * </ul>
 * 切面顺序设置在事务切面外层，因此业务方法抛异常或事务提交失败时，不会执行缓存同步。
 */
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 100)
public class SmartCacheAspect {

    private static final Logger log = LoggerFactory.getLogger(SmartCacheAspect.class);

    private final RedisCacheExecutor cacheExecutor;
    private final CacheKeyGenerator keyGenerator;
    private final SmartCacheProperties properties;
    private final ObjectMapper objectMapper;
    private final AfterCommitExecutor afterCommitExecutor;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public SmartCacheAspect(RedisCacheExecutor cacheExecutor,
                            CacheKeyGenerator keyGenerator,
                            SmartCacheProperties properties,
                            ObjectMapper objectMapper,
                            AfterCommitExecutor afterCommitExecutor) {
        this.cacheExecutor = cacheExecutor;
        this.keyGenerator = keyGenerator;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.afterCommitExecutor = afterCommitExecutor;
    }

    /**
     * 处理查询缓存。
     * <ol>
     *   <li>通过 SpEL 计算 Key 并查询 Redis；正常值或空值命中时直接返回。</li>
     *   <li>Redis 异常时直接回源 DB；关闭互斥锁时也直接回源。</li>
     *   <li>未命中时竞争 Redis 锁；持锁者二次检查缓存后查询 DB 并回填。</li>
     *   <li>未获得锁的请求短暂随机退避，等待持锁者完成缓存重建。</li>
     *   <li>超过最大等待时间仍未命中时回源 DB，保证请求不会无限阻塞。</li>
     * </ol>
     */
    @Around("@annotation(annotation)")
    public Object cacheable(ProceedingJoinPoint joinPoint, SmartCacheable annotation) throws Throwable {
        Method method = targetMethod(joinPoint);
        EvaluationContext context = context(joinPoint, method, null);
        String cacheKey = keyGenerator.generate(annotation.namespace(), evaluate(annotation.key(), context));
        JavaType returnType = objectMapper.getTypeFactory().constructType(method.getGenericReturnType());

        // 第一次查询：命中正常对象或空值标记时，可以立即结束请求。
        CacheLookup lookup = cacheExecutor.get(cacheKey);
        CacheResolution resolution = resolve(lookup, returnType, cacheKey);
        if (resolution.resolved()) {
            return resolution.value();
        }
        if (lookup.status() == CacheLookup.Status.ERROR || !annotation.mutex()) {
            return loadAndCache(joinPoint, annotation, cacheKey);
        }

        // 缓存未命中时，每个业务 Key 使用一把独立锁，避免不同数据之间互相阻塞。
        String lockKey = keyGenerator.lockKey(cacheKey);
        String owner = UUID.randomUUID().toString();
        if (cacheExecutor.tryLock(lockKey, owner)) {
            try {
                // 获取锁期间其他请求可能已重建缓存，所以访问 DB 前必须二次检查。
                CacheLookup secondLookup = cacheExecutor.get(cacheKey);
                CacheResolution secondResolution = resolve(secondLookup, returnType, cacheKey);
                if (secondResolution.resolved()) {
                    return secondResolution.value();
                }
                return loadAndCache(joinPoint, annotation, cacheKey);
            } finally {
                cacheExecutor.unlock(lockKey, owner);
            }
        }

        // 没抢到锁说明其他实例大概率正在重建；随机退避可以减少同时轮询 Redis 的压力。
        long deadline = System.nanoTime() + properties.getMaxWait().toNanos();
        while (System.nanoTime() < deadline) {
            TimeUnit.MILLISECONDS.sleep(ThreadLocalRandom.current().nextLong(30, 81));
            CacheLookup retry = cacheExecutor.get(cacheKey);
            CacheResolution retryResolution = resolve(retry, returnType, cacheKey);
            if (retryResolution.resolved()) {
                return retryResolution.value();
            }
            if (retry.status() == CacheLookup.Status.ERROR) {
                break;
            }
        }
        return loadAndCache(joinPoint, annotation, cacheKey);
    }

    /**
     * 处理 DB 更新后的缓存写入。
     * 先执行原业务方法，只有非空结果才计算 Key 并注册缓存写入；事务回滚时 afterCommit 不会触发。
     * Key 表达式可使用 {@code #result}，例如 {@code #result.id}。
     */
    @Around("@annotation(annotation)")
    public Object put(ProceedingJoinPoint joinPoint, SmartCachePut annotation) throws Throwable {
        Object result = joinPoint.proceed();
        if (result == null) {
            return null;
        }
        Method method = targetMethod(joinPoint);
        EvaluationContext context = context(joinPoint, method, result);
        String cacheKey = keyGenerator.generate(annotation.namespace(), evaluate(annotation.key(), context));
        afterCommitExecutor.execute(() -> cacheExecutor.put(
                cacheKey, result, annotation.ttlSeconds(), annotation.randomTtlSeconds()));
        return result;
    }

    /**
     * 处理 DB 删除后的缓存清理。
     * <p>
     * 缓存 Key 在调用业务方法前计算并固定，避免业务方法修改入参后删错 Key。业务返回
     * {@code false} 表示 DB 没有发生删除，此时不清缓存；成功后则注册为事务提交后删除。
     */
    @Around("@annotation(annotation)")
    public Object evict(ProceedingJoinPoint joinPoint, SmartCacheEvict annotation) throws Throwable {
        Method method = targetMethod(joinPoint);
        EvaluationContext context = context(joinPoint, method, null);
        String cacheKey = keyGenerator.generate(annotation.namespace(), evaluate(annotation.key(), context));

        Object result = joinPoint.proceed();

        // Boolean false 代表业务删除失败或目标不存在，Redis 无需变化。
        if (result instanceof Boolean && !(boolean)result) {
            return false;
        }
        // 有事务时提交后执行；无事务时立即执行。
        afterCommitExecutor.execute(() -> cacheExecutor.evict(cacheKey));
        return result;
    }

    /**
     * 执行真实查询方法并回填缓存。
     * DB 返回 null 时按注解决定是否写入短期空值；正常数据使用带随机抖动的 TTL。
     */
    private Object loadAndCache(ProceedingJoinPoint joinPoint,
                                SmartCacheable annotation,
                                String cacheKey) throws Throwable {
        Object value = joinPoint.proceed();
        if (value == null) {
            if (annotation.cacheNull()) {
                cacheExecutor.putNull(cacheKey, annotation.nullTtlSeconds());
            }
        } else {
            cacheExecutor.put(cacheKey, value, annotation.ttlSeconds(), annotation.randomTtlSeconds());
        }
        return value;
    }

    /**
     * 将 Redis 查询结果解析为切面可直接返回的结果。
     * 空值标记属于“已解析且值为 null”；JSON 损坏时删除坏缓存并返回未解析，让调用方重建。
     */
    private CacheResolution resolve(CacheLookup lookup, JavaType returnType, String cacheKey) {
        if (lookup.status() == CacheLookup.Status.NULL_VALUE) {
            return CacheResolution.resolved(null);
        }
        if (lookup.status() != CacheLookup.Status.HIT) {
            return CacheResolution.unresolved();
        }
        try {
            return CacheResolution.resolved(cacheExecutor.deserialize(lookup.value(), returnType));
        } catch (Exception ex) {
            log.warn("Cached value cannot be deserialized and will be rebuilt, key={}", cacheKey, ex);
            cacheExecutor.evict(cacheKey);
            return CacheResolution.unresolved();
        }
    }

    /** 创建包含方法参数和 {@code #result} 的 SpEL 上下文。 */
    private EvaluationContext context(ProceedingJoinPoint joinPoint, Method method, Object result) {
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
                joinPoint.getTarget(), method, joinPoint.getArgs(), parameterNameDiscoverer);
        context.setVariable("result", result);
        return context;
    }

    /** 执行注解中声明的 SpEL 表达式并返回业务 Key。 */
    private Object evaluate(String expression, EvaluationContext context) {
        return expressionParser.parseExpression(expression).getValue(context);
    }

    /**
     * 获取目标实现类上的具体方法，而不是代理接口方法，确保返回类型和参数名解析准确。
     */
    private Method targetMethod(ProceedingJoinPoint joinPoint) {
        Method signatureMethod = ((MethodSignature) joinPoint.getSignature()).getMethod();
        return AopUtils.getMostSpecificMethod(signatureMethod, joinPoint.getTarget().getClass());
    }



    /**
     * 切面内部的缓存解析结果。
     * resolved=true 表示缓存已经给出最终答案；其 value 可以为 null（命中了空值标记）。
     */
    private static final class CacheResolution {

        private final boolean resolved;
        private final Object value;

        private CacheResolution(boolean resolved, Object value) {
            this.resolved = resolved;
            this.value = value;
        }

        private boolean resolved() {
            return resolved;
        }

        private Object value() {
            return value;
        }


        private static CacheResolution resolved(Object value) {
            return new CacheResolution(true, value);
        }

        private static CacheResolution unresolved() {
            return new CacheResolution(false, null);
        }
    }
}
