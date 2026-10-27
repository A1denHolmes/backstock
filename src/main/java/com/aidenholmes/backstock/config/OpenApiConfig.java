package com.aidenholmes.backstock.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Backstock API")
                        .version("v1")
                        .description("""
                REST API for the Backstock online store.

                Errors are returned as Problem Details (RFC 9457) with the `application/problem+json` content type.
                Validation errors list the invalid fields in `errors`. Timestamps are in UTC in ISO 8601 format.
                Requests that update or delete a resource must send the `ETag` from the latest read of that resource in
                the `If-Match` header. Without the header the response is 428, with an outdated value 412.
                """)
                        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")));
    }
}
