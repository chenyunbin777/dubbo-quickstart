package org.apache.dubbo.samples.quickstart.cache.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 智能缓存模块的 Spring 配置入口。
 * <p>
 * 负责启用 {@link SmartCacheProperties} 配置绑定。其余缓存组件通过 Spring 组件扫描自动注册。
 */
@Configuration
@EnableConfigurationProperties(SmartCacheProperties.class)
public class SmartCacheConfig {
}
