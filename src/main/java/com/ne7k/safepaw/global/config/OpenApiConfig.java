package com.ne7k.safepaw.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customopenAPI() {
        return new OpenAPI()
                .info(new Info() // swagger
                        .title("SafePaw API")
                        .description("SafePaw REST API")
                        .version("v1")
                )
                .components(new Components().addSecuritySchemes("bearerAuth", // jwt 토큰 복사 버튼
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
    }
}
