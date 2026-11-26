package com.acme.coldtrace.platform.shared.infrastructure.documentation.openapi.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI metadata configuration.
 *
 * @since 1.0.0
 */
@Configuration
public class OpenApiConfiguration {
    private static final String BEARER_AUTH_SCHEME = "bearerAuth";
    private static final String SWAGGER_AUTHENTICATION_GUIDE = """

            ### Swagger authentication workflow

            1. Execute `POST /api/v1/authentication/sign-in` with an existing ColdTrace user.
            2. Copy the `token` value returned by the response.
            3. Click **Authorize** and paste only the token value in `bearerAuth`.
            4. Use the returned `organizationId` when testing organization-scoped endpoints.

            Protected endpoints return `401` when the Bearer token is missing, expired, or invalid.
            Public endpoints such as authentication, Swagger/OpenAPI, subscription plans, and Stripe
            webhooks do not require authorization.
            """;

    @Bean
    public OpenAPI coldTraceOpenApi(
            @Value("${spring.application.name:ColdTrace Platform}") String applicationName,
            @Value("${documentation.application.description:ColdTrace Platform REST API}") String applicationDescription,
            @Value("${documentation.application.version:1.0.0}") String applicationVersion,
            @Value("${documentation.servers.local-url:http://localhost:8080}") String localServerUrl,
            @Value("${documentation.servers.production-url:https://coldtrace-platform-dtbzbm7bta-uc.a.run.app}") String productionServerUrl
    ) {
        var info = new Info()
                .title(applicationName + " API")
                .description(applicationDescription + SWAGGER_AUTHENTICATION_GUIDE)
                .version(applicationVersion)
                .contact(new Contact()
                        .name("ICEQ ColdTrace Team")
                        .url("https://github.com/ICEQ2026/coldtrace-platform"))
                .license(new License()
                        .name("MIT")
                        .url("https://opensource.org/license/mit"));

        var bearerSecurityScheme = new SecurityScheme()
                .name("Authorization")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT returned by POST /api/v1/authentication/sign-in. In Swagger UI, paste only the raw token value; Swagger sends it as Authorization: Bearer <token>.");

        return new OpenAPI()
                .info(info)
                .servers(List.of(
                        new Server()
                                .url("/")
                                .description("Current Swagger UI host"),
                        new Server()
                                .url(localServerUrl)
                                .description("Local development"),
                        new Server()
                                .url(productionServerUrl)
                                .description("Google Cloud Run production")
                ))
                .externalDocs(new ExternalDocumentation()
                        .description("ColdTrace backend README")
                        .url("https://github.com/ICEQ2026/coldtrace-platform#readme"))
                .components(new Components().addSecuritySchemes(BEARER_AUTH_SCHEME, bearerSecurityScheme))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH_SCHEME));
    }
}
