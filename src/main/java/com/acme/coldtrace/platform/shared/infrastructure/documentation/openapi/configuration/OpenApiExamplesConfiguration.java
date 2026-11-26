package com.acme.coldtrace.platform.shared.infrastructure.documentation.openapi.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Enriches generated OpenAPI schemas with concrete examples and descriptions.
 *
 * @since 1.0.0
 */
@Configuration
public class OpenApiExamplesConfiguration {
    /**
     * Adds example values to schemas, request bodies, responses, and common path parameters.
     *
     * @return OpenAPI customizer
     */
    @Bean
    public OpenApiCustomizer coldTraceExampleOpenApiCustomizer() {
        return openApi -> {
            if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) return;

            var schemas = openApi.getComponents().getSchemas();
            schemas.forEach((schemaName, schema) -> enrichSchema(schemaName, schema, schemas));
            enrichOperations(openApi, schemas);
        };
    }

    private static void enrichSchema(String schemaName, Schema<?> schema, Map<String, Schema> schemas) {
        if (schema == null) return;
        if (isBlank(schema.getDescription())) {
            schema.setDescription("Example payload for " + readableName(schemaName) + ".");
        }
        if ("ProblemDetail".equals(schemaName)) {
            enrichProblemDetailSchema(schema);
            return;
        }

        if (schema.getProperties() != null) {
            schema.getProperties().forEach((propertyName, propertySchema) -> enrichProperty(propertyName, propertySchema));
        }

        if (schema.getExample() == null) {
            schema.setExample(exampleForSchema(schema, schemas, new HashSet<>()));
        }
    }

    private static void enrichProblemDetailSchema(Schema<?> schema) {
        if (schema.getProperties() != null) {
            setPropertyExample(schema, "type", "https://coldtrace.app/problems/validation-error");
            setPropertyExample(schema, "title", "Request validation failed");
            setPropertyExample(schema, "status", 400);
            setPropertyExample(schema, "detail", "The request payload contains invalid or missing fields.");
            setPropertyExample(schema, "instance", "/api/v1/organizations/1/assets");
            setPropertyExample(schema, "properties", Map.of("field", "name"));
            schema.getProperties().forEach((propertyName, propertySchema) -> {
                if (propertySchema instanceof Schema<?> property && isBlank(property.getDescription())) {
                    property.setDescription(descriptionForProperty(propertyName));
                }
            });
        }
        if (schema.getExample() == null) {
            schema.setExample(problemDetailExample("400"));
        }
    }

    private static void setPropertyExample(Schema<?> schema, String propertyName, Object example) {
        var propertySchema = schema.getProperties().get(propertyName);
        if (propertySchema instanceof Schema<?> property) {
            property.setExample(example);
        }
    }

    private static void enrichProperty(String propertyName, Schema<?> propertySchema) {
        if (propertySchema == null) return;
        if (isBlank(propertySchema.getDescription())) {
            propertySchema.setDescription(descriptionForProperty(propertyName));
        }
        if (propertySchema.getExample() == null) {
            propertySchema.setExample(exampleForProperty(propertyName, propertySchema));
        }
    }

    private static void enrichOperations(OpenAPI openApi, Map<String, Schema> schemas) {
        if (openApi.getPaths() == null) return;

        openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperationsMap().forEach((httpMethod, operation) -> {
            if (operation == null) return;
            if (isBlank(operation.getDescription())) {
                operation.setDescription(descriptionForOperation(httpMethod, path, operation));
            }
            enrichParameters(operation.getParameters());
            enrichRequestBody(path, httpMethod, operation, schemas);
            enrichResponses(path, httpMethod, operation, schemas);
        }));
    }

    private static void enrichParameters(List<Parameter> parameters) {
        if (parameters == null) return;
        parameters.forEach(parameter -> {
            if (parameter == null) return;
            if (isBlank(parameter.getDescription())) {
                parameter.setDescription(descriptionForProperty(parameter.getName()));
            }
            if (parameter.getExample() == null) {
                parameter.setExample(exampleForParameter(parameter.getName()));
            }
        });
    }

    private static void enrichRequestBody(
            String path,
            PathItem.HttpMethod httpMethod,
            Operation operation,
            Map<String, Schema> schemas
    ) {
        if (operation.getRequestBody() == null) return;
        if (isBlank(operation.getRequestBody().getDescription())) {
            operation.getRequestBody().setDescription("Request payload for " + readableOperation(httpMethod, path) + ".");
        }
        enrichContent(operation.getRequestBody().getContent(), schemas, path, true);
    }

    private static void enrichResponses(
            String path,
            PathItem.HttpMethod httpMethod,
            Operation operation,
            Map<String, Schema> schemas
    ) {
        if (operation.getResponses() == null) return;
        operation.getResponses().forEach((statusCode, response) -> enrichResponse(path, httpMethod, statusCode, response, schemas));
    }

    private static void enrichResponse(
            String path,
            PathItem.HttpMethod httpMethod,
            String statusCode,
            ApiResponse response,
            Map<String, Schema> schemas
    ) {
        if (response == null) return;
        if (isBlank(response.getDescription())) {
            response.setDescription("Response returned by " + readableOperation(httpMethod, path) + ".");
        }
        enrichContent(response.getContent(), schemas, path, false);
    }

    private static void enrichContent(Content content, Map<String, Schema> schemas, String path, boolean request) {
        if (content == null) return;
        content.values().forEach(mediaType -> enrichMediaType(mediaType, schemas, path, request));
    }

    private static void enrichMediaType(MediaType mediaType, Map<String, Schema> schemas, String path, boolean request) {
        if (mediaType == null || mediaType.getExample() != null) return;
        var example = request && path.contains("/billing/stripe/webhooks")
                ? stripeWebhookEventExample()
                : exampleForSchema(mediaType.getSchema(), schemas, new HashSet<>());
        if (example != null) {
            mediaType.setExample(example);
        }
    }

    private static Object exampleForSchema(Schema<?> schema, Map<String, Schema> schemas, Set<String> seenRefs) {
        if (schema == null) return null;
        if (schema.getExample() != null) return schema.getExample();

        var ref = schema.get$ref();
        if (!isBlank(ref)) {
            var schemaName = ref.substring(ref.lastIndexOf('/') + 1);
            if (!seenRefs.add(schemaName)) return readableName(schemaName);
            var referencedSchema = schemas.get(schemaName);
            return referencedSchema == null ? readableName(schemaName) : exampleForSchema(referencedSchema, schemas, seenRefs);
        }

        if (schema.getItems() != null || "array".equals(schema.getType())) {
            var itemExample = exampleForSchema(schema.getItems(), schemas, seenRefs);
            return itemExample == null ? List.of() : List.of(itemExample);
        }

        if (schema.getProperties() != null && !schema.getProperties().isEmpty()) {
            var objectExample = new LinkedHashMap<String, Object>();
            schema.getProperties().forEach((propertyName, propertySchema) ->
                    objectExample.put(propertyName, exampleForNestedProperty(propertyName, propertySchema, schemas, seenRefs)));
            return objectExample;
        }

        return exampleForType(schema.getType(), schema.getFormat());
    }

    private static Object exampleForProperty(String propertyName, Schema<?> schema) {
        var lowerName = propertyName == null ? "" : propertyName.toLowerCase(Locale.ROOT);
        if (schema != null && schema.getItems() != null) {
            return List.of(exampleForProperty(singular(lowerName), schema.getItems()));
        }
        if (lowerName.equals("type") && schema != null && "uri".equals(schema.getFormat())) {
            return "https://coldtrace.app/problems/validation-error";
        }
        if (lowerName.equals("status") && schema != null && "integer".equals(schema.getType())) {
            return 400;
        }
        if (lowerName.equals("title")) return "Request validation failed";
        if (lowerName.equals("detail")) return "The request payload contains invalid or missing fields.";
        if (lowerName.equals("instance")) return "/api/v1/organizations/1/assets";
        if (lowerName.contains("modelprovider")) return "openai";
        if (lowerName.contains("modelname")) return "gpt-4o-mini";
        if (lowerName.endsWith("id") || lowerName.equals("id")) return 1L;
        if (lowerName.equals("uuid")) return "CT-000001";
        if (lowerName.contains("email")) return "operations@coldtrace.test";
        if (lowerName.contains("password")) return "ColdTrace123";
        if (lowerName.contains("token")) return "eyJhbGciOiJIUzUxMiJ9...";
        if (lowerName.contains("authorizationcode")) return "4/0AfJohXn...";
        if (lowerName.contains("redirecturi")) return "https://coldtrace-frontend-liard.vercel.app/identity-access/sign-in";
        if (lowerName.contains("url")) return "https://checkout.stripe.com/c/pay/cs_test_a1B2c3";
        if (lowerName.contains("provider")) return lowerName.contains("metadata") ? "provider=test;model=gpt-4o-mini" : "STRIPE";
        if (lowerName.contains("plan")) return lowerName.contains("code") ? "operations" : "Operations";
        if (lowerName.contains("currency")) return "PEN";
        if (lowerName.contains("status")) return "active";
        if (lowerName.contains("severity")) return "critical";
        if (lowerName.contains("priority")) return "high";
        if (lowerName.contains("type")) return "FREEZER";
        if (lowerName.contains("name")) return "Freezer A1";
        if (lowerName.contains("title")) return "June compliance summary";
        if (lowerName.contains("description")) return "Cold-chain monitoring asset used for temperature-sensitive inventory.";
        if (lowerName.contains("message")) return "Temperature exceeded configured safe range.";
        if (lowerName.contains("summary")) return "Temperature recovered after inventory transfer and device verification.";
        if (lowerName.contains("evidence")) return "30 minutes of stable readings and technician notes.";
        if (lowerName.contains("recommendation")) return "Review the affected asset and confirm corrective evidence.";
        if (lowerName.contains("action")) return "Move inventory to backup freezer and inspect the sensor.";
        if (lowerName.contains("reason")) return "Temperature remained above the configured threshold.";
        if (lowerName.contains("notes")) return "Operator confirmed the corrective action and stable readings.";
        if (lowerName.contains("question")) return "What should I review first?";
        if (lowerName.contains("language")) return "es";
        if (lowerName.contains("role")) return "SUPER_ADMIN";
        if (lowerName.contains("resource")) return "assets";
        if (lowerName.contains("channel")) return "EMAIL";
        if (lowerName.contains("recipient")) return "operations@coldtrace.test";
        if (lowerName.contains("location")) return "Main Warehouse";
        if (lowerName.contains("gateway")) return "Gateway Lima 01";
        if (lowerName.contains("asset")) return "Freezer A1";
        if (lowerName.contains("device")) return "Temperature Sensor A1";
        if (lowerName.contains("temperature")) return 5.2;
        if (lowerName.contains("humidity")) return 48.0;
        if (lowerName.contains("capacity")) return 450.0;
        if (lowerName.contains("percentage") || lowerName.contains("compliance")) return 92.5;
        if (lowerName.contains("price") || lowerName.contains("cents")) return 14900;
        if (lowerName.contains("count") || lowerName.contains("days") || lowerName.contains("limit") ||
                lowerName.contains("remaining") || lowerName.contains("used") || lowerName.contains("sequence") ||
                lowerName.contains("frequency") || lowerName.contains("retention")) {
            return 10;
        }
        if (lowerName.startsWith("is") || lowerName.startsWith("has") || lowerName.contains("enabled") ||
                lowerName.contains("allows") || lowerName.contains("visible") || lowerName.contains("recommended") ||
                lowerName.contains("accepted") || lowerName.contains("duplicate") || lowerName.contains("cancel")) {
            return true;
        }
        if (lowerName.contains("date") || lowerName.endsWith("at") || lowerName.contains("start") || lowerName.contains("end")) {
            return "2026-06-20T09:00:00Z";
        }
        if (lowerName.contains("period")) return "2026-06";
        if (lowerName.contains("metadata")) return "source=stripe-test";
        if (lowerName.contains("code")) return "CT-OPS";
        if (lowerName.contains("key")) return "aiGuidance";
        if (lowerName.contains("label")) return "Recommended";
        if (lowerName.contains("unit")) return "C";
        if (lowerName.contains("metric")) return "openIncidents";
        if (lowerName.contains("value")) return "10.5 C";
        if (lowerName.contains("area")) return "Thermal compliance";

        return schema == null ? "example" : exampleForType(schema.getType(), schema.getFormat());
    }

    private static Object exampleForNestedProperty(
            String propertyName,
            Schema<?> propertySchema,
            Map<String, Schema> schemas,
            Set<String> seenRefs
    ) {
        if (propertySchema == null) return exampleForProperty(propertyName, null);
        if (!isBlank(propertySchema.get$ref()) || propertySchema.getProperties() != null || propertySchema.getItems() != null) {
            var nestedExample = exampleForSchema(propertySchema, schemas, new HashSet<>(seenRefs));
            if (nestedExample != null) return nestedExample;
        }
        return exampleForProperty(propertyName, propertySchema);
    }

    private static Object exampleForType(String type, String format) {
        if ("integer".equals(type)) return 1;
        if ("number".equals(type)) return 5.2;
        if ("boolean".equals(type)) return true;
        if ("array".equals(type)) return List.of("example");
        if ("date-time".equals(format)) return "2026-06-20T09:00:00Z";
        if ("date".equals(format)) return "2026-06-20";
        return "example";
    }

    private static Object exampleForParameter(String name) {
        var lowerName = name == null ? "" : name.toLowerCase(Locale.ROOT);
        if (lowerName.equals("provider")) return "google";
        if (lowerName.contains("signature")) {
            return "t=1720000000,v1=5257a869e7...";
        }
        return lowerName.endsWith("id") || lowerName.equals("id") ? 1L : "example";
    }

    private static String descriptionForProperty(String propertyName) {
        if (isBlank(propertyName)) return "Example field value.";
        return readableName(propertyName) + " value used by the ColdTrace API.";
    }

    private static String descriptionForOperation(PathItem.HttpMethod httpMethod, String path, Operation operation) {
        return Optional.ofNullable(operation.getSummary())
                .filter(summary -> !isBlank(summary))
                .map(summary -> summary + ".")
                .orElse("Executes " + readableOperation(httpMethod, path) + ".");
    }

    private static String readableOperation(PathItem.HttpMethod httpMethod, String path) {
        return httpMethod.name() + " " + path;
    }

    private static String readableName(String value) {
        if (isBlank(value)) return "resource";
        var spaced = value.replaceAll("([a-z])([A-Z])", "$1 $2")
                .replace('-', ' ')
                .replace('_', ' ')
                .trim();
        return spaced.isEmpty() ? "resource" : spaced.substring(0, 1).toUpperCase(Locale.ROOT) + spaced.substring(1);
    }

    private static String singular(String value) {
        return value != null && value.endsWith("s") ? value.substring(0, value.length() - 1) : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static Object stripeWebhookEventExample() {
        var dataObject = new LinkedHashMap<String, Object>();
        dataObject.put("id", "sub_1Tm6ZCD8yEVnLFXakRBz1IUu");
        dataObject.put("customer", "cus_L82G0DAIEgLtBf");
        dataObject.put("status", "active");
        dataObject.put("metadata", Map.of(
                "organizationId", "1",
                "planCode", "operations"
        ));

        var data = new LinkedHashMap<String, Object>();
        data.put("object", dataObject);

        var event = new LinkedHashMap<String, Object>();
        event.put("id", "evt_1Tm6ZED8yEVnLFXaZ9f3u7rM");
        event.put("object", "event");
        event.put("type", "customer.subscription.updated");
        event.put("data", data);
        return event;
    }

    private static Object problemDetailExample(String statusCode) {
        var status = "default".equals(statusCode) ? 400 : Integer.parseInt(statusCode);
        var problem = new LinkedHashMap<String, Object>();
        problem.put("type", "https://coldtrace.app/problems/validation-error");
        problem.put("title", status >= 500 ? "Service unavailable" : "Request validation failed");
        problem.put("status", status);
        problem.put("detail", status >= 500
                ? "The service could not complete the request at this time."
                : "The request payload contains invalid or missing fields.");
        problem.put("instance", "/api/v1/organizations/1/assets");
        problem.put("properties", Map.of("field", "name"));
        return problem;
    }
}
