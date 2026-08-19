package org.apache.dubbo.samples.quickstart.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询缓存注解。
 * <p>
 * 标注在“根据参数查询数据”的方法上。切面会先查询 Redis；未命中时执行原方法查询 DB，
 * 并把结果写回 Redis。它同时支持空值缓存、TTL 随机抖动和 Redis 互斥锁，分别用于缓解
 * 缓存穿透、缓存雪崩和热点 Key 击穿问题。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SmartCacheable {

    /** 业务命名空间，例如 {@code user}，用于隔离不同类型的缓存。 */
    String namespace();

    /** 缓存业务 Key 的 SpEL 表达式，例如 {@code #id}。 */
    String key();

    /** 正常缓存的固定 TTL（秒）；小于等于 0 时使用全局配置。 */
    long ttlSeconds() default -1;

    /** 追加到固定 TTL 上的随机秒数上限；-1 表示使用全局配置，0 表示不抖动。 */
    long randomTtlSeconds() default -1;

    /** DB 未查询到数据时是否写入短期空值标记，用于阻止相同无效请求反复访问 DB。 */
    boolean cacheNull() default true;

    /** 空值标记的 TTL（秒）；小于等于 0 时使用全局配置。 */
    long nullTtlSeconds() default -1;

    /** 缓存未命中时是否使用 Redis 互斥锁，只允许一个请求重建热点缓存。 */
    boolean mutex() default true;
}
