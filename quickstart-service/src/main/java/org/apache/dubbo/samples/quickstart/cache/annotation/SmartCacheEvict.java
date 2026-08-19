package org.apache.dubbo.samples.quickstart.cache.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 删除缓存注解。
 * <p>
 * 适用于删除 DB 数据的方法。切面在调用业务方法前固定缓存 Key，在业务执行成功并且事务
 * 成功提交后删除 Redis 缓存；若业务方法返回 {@code false}、抛出异常或事务回滚，则保留缓存。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SmartCacheEvict {

    /** 业务命名空间，例如 {@code user}。 */
    String namespace();

    /** 缓存 Key 的 SpEL 表达式，例如 {@code #id}。 */
    String key();
}
