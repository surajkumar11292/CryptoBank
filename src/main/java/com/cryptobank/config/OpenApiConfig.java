package com.cryptobank.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CryptoBank REST API")
                        .version("1.0.0")
                        .description("Production Spring Boot 3.3.4 & Java 21 Banking System — Accounts, Double-Entry Ledger, Transfers, 2FA, Debit Cards, and Admin Console.")
                        .contact(new Contact().name("CryptoBank Engineering Team"))
                        .license(new License().name("MIT")));
    }
}
