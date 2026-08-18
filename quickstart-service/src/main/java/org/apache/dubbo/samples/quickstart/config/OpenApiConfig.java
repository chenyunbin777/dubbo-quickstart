package org.apache.dubbo.samples.quickstart.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 接口文档配置。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI quickStartOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Dubbo Quickstart 接口文档")
                .description("提供用户管理和 Dubbo 调用测试接口")
                .version("v1.0.0")
                .contact(new Contact().name("Dubbo Quickstart")));
    }
}
