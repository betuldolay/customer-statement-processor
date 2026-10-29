package com.betul.statementprocessor.api;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI statementProcessorOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Customer Statement Processor API")
                .description("Validates customer statement records delivered as CSV or XML files.")
                .version("1.0"));
    }
}