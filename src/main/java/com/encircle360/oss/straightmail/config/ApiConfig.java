package com.encircle360.oss.straightmail.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

// Swagger UI uses the current request host as the server URL, so straightmail
// can be reached through reverse proxies, port forwarding or tunnels without
// a hard-coded base URL.
@OpenAPIDefinition(
    info = @Info(
        title = "Straightmail",
        version = "0.4.0",
        description = ""
    )
)
@Configuration
public class ApiConfig {
}
