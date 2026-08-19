package org.apache.dubbo.samples.quickstart.cache.core;

import java.util.Objects;

/**
 * 一次 Redis 查询的结构化结果。
 * <p>
 * 不能只用 {@code null} 表示查询结果，因为 {@code null} 无法区分“Key 不存在”、
 * “命中了主动缓存的空值”和“Redis 访问异常”。切面根据这里的状态决定返回空值、访问 DB
 * 或执行降级。该类通过构造器校验保证 HIT 必须有值，其他状态不能携带值。
 */
public final class CacheLookup {

    /** Redis 查询可能产生的四种互斥状态。 */
    public enum Status {
        /** 命中正常 JSON 数据。 */
        HIT, NULL_VALUE, MISS, ERROR
    }
    private final Status status;
    private final String value;

    /**
     * 创建并校验查询结果。
     *
     * @param status 查询状态，不能为空
     * @param value  仅 HIT 状态允许携带的原始 JSON 字符串
     */
    public CacheLookup(Status status, String value) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        if (status == Status.HIT && value == null) {
            throw new IllegalArgumentException("A cache hit must contain a value");
        }
        if (status != Status.HIT && value != null) {
            throw new IllegalArgumentException("Only a cache hit may contain a value");
        }
        this.value = value;
    }

    /** 返回本次查询状态。 */
    public Status status() {
        return status;
    }

    /** 返回命中时的原始 JSON；非 HIT 状态返回 {@code null}。 */
    public String value() {
        return value;
    }

    /** 创建正常命中结果。 */
    public static CacheLookup hit(String value) {
        return new CacheLookup(Status.HIT, Objects.requireNonNull(value, "value must not be null"));
    }

    /** 创建 MISS、NULL_VALUE 或 ERROR 结果；HIT 必须通过 {@link #hit(String)} 创建。 */
    public static CacheLookup of(Status status) {
        if (status == Status.HIT) {
            throw new IllegalArgumentException("Use hit(value) to create a cache hit");
        }
        return new CacheLookup(status, null);
    }
}
