package org.apache.dubbo.samples.quickstart.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 更新缓存注解。
 * <p>
 * 标注在更新 DB 并返回最新对象的方法上。原方法成功返回非空结果后，切面根据返回值或
 * 方法参数计算缓存 Key，并在当前事务成功提交后把结果写入 Redis。事务回滚时不会更新缓存。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SmartCachePut {

    /** 业务命名空间，例如 {@code user}。 */
    String namespace();

    /** 缓存 Key 的 SpEL 表达式，可引用返回值，例如 {@code #result.id}。 */
    String key();

    /** 缓存固定 TTL（秒）；小于等于 0 时使用全局配置。 */
    long ttlSeconds() default -1;

    /** TTL 随机抖动秒数上限；-1 表示使用全局配置，0 表示不抖动。 */
    long randomTtlSeconds() default -1;
}
