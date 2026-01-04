package com.tradeflow.oms.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 3.0 Configuration for OMS Service
 * Provides Swagger UI documentation and API spec generation
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI omsServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TradeFlow OMS Service API")
                        .description("Order Management System with Saga orchestration for distributed transactions")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TradeFlow Team")
                                .email("support@tradeflow.com")
                                .url("https://github.com/tradeflow/platform"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .addServersItem(new Server()
                        .url("http://localhost:8083")
                        .description("Local Development Server"))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT authentication token. Obtain via /api/auth/login or /api/auth/register")));
    }
}
