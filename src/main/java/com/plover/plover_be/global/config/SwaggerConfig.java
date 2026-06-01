package com.plover.plover_be.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .addServersItem(new Server()
                        .url("https://moong.co.kr")
                        .description("Production Server"))
                .addServersItem(new Server()
                        .url("http://localhost:8080")
                        .description("Local Server"))
                .info(new Info()
                        .title("Plover API")
                        .version("v1.0")
                        .description("Plover 서비스 API 문서"));
    }
}
