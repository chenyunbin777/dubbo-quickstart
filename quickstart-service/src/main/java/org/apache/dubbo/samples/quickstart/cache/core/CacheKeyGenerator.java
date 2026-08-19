package org.apache.dubbo.samples.quickstart.cache.core;

import org.apache.dubbo.samples.quickstart.cache.config.SmartCacheProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Redis 缓存 Key 生成器。
 * <p>
 * 将应用前缀、业务命名空间、缓存结构版本和业务 Key 组合成统一格式：
 * {@code 应用前缀:命名空间:v1:业务Key}。版本段便于将来缓存结构变化时整体切换。
 */
@Component
public class CacheKeyGenerator {

    private final SmartCacheProperties properties;

    public CacheKeyGenerator(SmartCacheProperties properties) {
        this.properties = properties;
    }

    /**
     * 生成业务缓存 Key，并拒绝空命名空间或空业务 Key，避免不同请求写入同一个错误 Key。
     */
    public String generate(String namespace, Object key) {
        String keyText = key == null ? "" : key.toString();
        if (!StringUtils.hasText(namespace) || !StringUtils.hasText(keyText)) {
            throw new IllegalArgumentException("Cache namespace and key must not be blank");
        }
        return properties.getKeyPrefix() + ":" + namespace + ":v1:" + keyText;
    }

    /** 为业务缓存 Key 生成独立的分布式锁 Key。 */
    public String lockKey(String cacheKey) {
        return "lock:" + cacheKey;
    }
}
