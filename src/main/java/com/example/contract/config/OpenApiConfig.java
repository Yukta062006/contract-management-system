package com.example.contract.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for OpenAPI 3 / Swagger documentation.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI contractOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Contract Management Backend System API")
                        .description("REST API for uploading contracts (PDF/DOCX), managing approval workflows, " +
                                "and asking questions using keyword search. Built for Seventh Ray Consulting and Technology Private Limited.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Internship Developer")
                                .email("intern@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")));
    }
}
