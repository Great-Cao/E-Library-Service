package com.example.elibrary.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI eLibraryOpenApi() {
        return new OpenAPI().info(new Info()
                .title("E-Library Service API")
                .version("v1")
                .description("""
                        Browse books, borrow and return copies, and list the active loans of the
                        simulated current user.

                        Identity is simulated with the `X-User-Id` request header and is not
                        authentication. Demo users are seeded with ids 1 and 2.
                        """));
    }
}
