package com.connecthub.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI Configuration for ConnectHub Auth Service
 * Provides API documentation accessible at: /swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("ConnectHub Auth Service API")
                        .version("1.0.0")
                        .description("Real-time Chat Application - Authentication Microservice\n\n" +
                                "This API provides authentication and user management services for ConnectHub.\n\n" +
                                "### Key Features:\n" +
                                "- User registration and login\n" +
                                "- JWT token generation and validation\n" +
                                "- User profile management\n" +
                                "- Password management\n" +
                                "- User search functionality\n" +
                                "- Status updates and presence tracking\n\n" +
                                "### Authentication:\n" +
                                "All protected endpoints require JWT Bearer token in Authorization header.\n\n" +
                                "### Base URL:\n" +
                                "http://localhost:8081/api/v1")
                        .contact(new Contact()
                                .name("ConnectHub Team")
                                .email("support@connecthub.com")
                                .url("https://connecthub.com")))
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT token")));
    }
}
