package org.apache.dubbo.samples.quickstart.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 智能缓存的全局默认配置，对应 {@code application.yml} 中的 {@code smart-cache} 节点。
 * <p>
 * 方法注解未显式指定 TTL 时使用这里的值；统一配置可以避免不同业务随意写死缓存参数。
 */
@ConfigurationProperties(prefix = "smart-cache")
@Data
public class SmartCacheProperties {

    /** 所有业务缓存 Key 的公共前缀，用于区分应用或环境。 */
    private String keyPrefix = "quickstart";
    /** 正常数据的默认存活时间。 */
    private Duration defaultTtl = Duration.ofMinutes(30);
    /** 正常 TTL 的随机增量上限，用来分散大量 Key 的过期时间。 */
    private Duration randomTtl = Duration.ofMinutes(5);
    /** DB 中不存在的数据对应的空值缓存时间。 */
    private Duration nullTtl = Duration.ofMinutes(1);
    /** 热点缓存重建锁的最长存活时间，防止持锁实例宕机后形成死锁。 */
    private Duration lockTtl = Duration.ofSeconds(10);
    /** 未获取重建锁的请求等待其他实例完成缓存重建的最长时间。 */
    private Duration maxWait = Duration.ofSeconds(3);


}
