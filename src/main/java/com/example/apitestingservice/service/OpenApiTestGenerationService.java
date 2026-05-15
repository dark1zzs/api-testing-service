package com.example.apitestingservice.service;

import com.example.apitestingservice.dto.ApiTestResponse;
import com.example.apitestingservice.dto.OpenApiGenerationRequest;
import com.example.apitestingservice.dto.OpenApiGenerationResponse;
import com.example.apitestingservice.dto.ProjectResponse;
import com.example.apitestingservice.entity.ApiTest;
import com.example.apitestingservice.entity.Project;
import com.example.apitestingservice.repository.ApiTestRepository;
import com.example.apitestingservice.repository.ProjectRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class OpenApiTestGenerationService {

    private static final List<String> SUPPORTED_METHODS = List.of("get", "post", "put", "delete");
    private static final Map<String, Object> DEFAULT_VALUES = Map.of(
            "string", "test",
            "integer", 1,
            "number", 1,
            "boolean", true
    );

    private final ProjectRepository projectRepository;
    private final ApiTestRepository apiTestRepository;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public OpenApiTestGenerationService(
            ProjectRepository projectRepository,
            ApiTestRepository apiTestRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper
    ) {
        this.projectRepository = projectRepository;
        this.apiTestRepository = apiTestRepository;
        this.restClient = restClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    public OpenApiGenerationResponse generate(OpenApiGenerationRequest request) {
        JsonNode specification = loadSpecification(request.openApiUrl());
        Project project = createProject(request);
        List<ApiTestResponse> tests = createTests(project, specification);

        return new OpenApiGenerationResponse(
                toProjectResponse(project),
                tests.size(),
                tests
        );
    }

    private JsonNode loadSpecification(String openApiUrl) {
        validateOpenApiUrl(openApiUrl);
        try {
            String body = restClient.get()
                    .uri(openApiUrl)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                throw new IllegalArgumentException("OpenAPI document is empty");
            }
            return objectMapper.readTree(body);
        } catch (RestClientException e) {
            throw new IllegalArgumentException("Cannot load OpenAPI document");
        } catch (Exception e) {
            throw new IllegalArgumentException("OpenAPI document is not valid JSON");
        }
    }

    private void validateOpenApiUrl(String openApiUrl) {
        try {
            URI uri = new URI(openApiUrl);
            String scheme = uri.getScheme();
            if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
                throw new IllegalArgumentException("OpenAPI URL must use http or https");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("OpenAPI URL is not valid");
        }
    }

    private Project createProject(OpenApiGenerationRequest request) {
        Project project = new Project();
        project.setName(request.projectName().trim());
        project.setBaseUrl(request.baseUrl().trim());
        project.setDescription(blankToNull(request.description()));
        return projectRepository.save(project);
    }

    private List<ApiTestResponse> createTests(Project project, JsonNode specification) {
        JsonNode paths = specification.path("paths");
        if (!paths.isObject()) {
            throw new IllegalArgumentException("OpenAPI document must contain paths object");
        }

        List<ApiTestResponse> generated = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> pathEntries = paths.fields();
        int runOrder = 0;

        while (pathEntries.hasNext()) {
            Map.Entry<String, JsonNode> pathEntry = pathEntries.next();
            String path = pathEntry.getKey();
            JsonNode pathNode = pathEntry.getValue();
            for (String method : SUPPORTED_METHODS) {
                JsonNode operation = pathNode.path(method);
                if (operation.isMissingNode()) {
                    continue;
                }

                ApiTest apiTest = buildTest(project, specification, path, pathNode, method, operation, runOrder);
                generated.add(toApiTestResponse(apiTestRepository.save(apiTest)));
                runOrder++;
            }
        }

        return generated;
    }

    private ApiTest buildTest(
            Project project,
            JsonNode specification,
            String path,
            JsonNode pathNode,
            String method,
            JsonNode operation,
            int runOrder
    ) {
        String httpMethod = method.toUpperCase(Locale.ROOT);
        int expectedStatus = chooseExpectedStatus(operation);
        String feature = firstText(operation.path("tags"), "General");
        String story = textOrFallback(operation.path("summary"), operation.path("operationId"), path);

        ApiTest apiTest = new ApiTest();
        apiTest.setProject(project);
        apiTest.setName(buildName(httpMethod, path, operation));
        apiTest.setDescription("Generated from OpenAPI specification");
        apiTest.setTestKey(buildTestKey(httpMethod, path, runOrder));
        apiTest.setFeature(feature);
        apiTest.setStory(story);
        apiTest.setMethod(httpMethod);
        apiTest.setEndpoint(buildEndpoint(path, specification, pathNode, operation));
        apiTest.setRequestBody(buildRequestBody(operation, specification));
        apiTest.setExpectedStatus(expectedStatus);
        apiTest.setRunOrder(runOrder);
        return apiTest;
    }

    private int chooseExpectedStatus(JsonNode operation) {
        JsonNode responses = operation.path("responses");
        if (!responses.isObject()) {
            return 200;
        }

        Iterator<String> names = responses.fieldNames();
        int fallback = 200;
        while (names.hasNext()) {
            String code = names.next();
            if (!code.matches("\\d{3}")) {
                continue;
            }
            int status = Integer.parseInt(code);
            if (status >= 200 && status < 300) {
                return status;
            }
            fallback = status;
        }

        return fallback;
    }

    private String buildEndpoint(String path, JsonNode specification, JsonNode pathNode, JsonNode operation) {
        String endpoint = fillPathParameters(path);
        String query = buildQueryString(specification, pathNode, operation);
        if (query.isBlank()) {
            return endpoint;
        }
        return endpoint + "?" + query;
    }

    private String fillPathParameters(String path) {
        return path.replaceAll("\\{[^/]+}", "1");
    }

    private String buildQueryString(JsonNode specification, JsonNode pathNode, JsonNode operation) {
        List<String> pairs = new ArrayList<>();
        collectQueryParameters(pathNode.path("parameters"), specification, pairs);
        collectQueryParameters(operation.path("parameters"), specification, pairs);
        return String.join("&", pairs);
    }

    private void collectQueryParameters(JsonNode parameters, JsonNode specification, List<String> pairs) {
        if (!parameters.isArray()) {
            return;
        }
        for (JsonNode parameter : parameters) {
            if (!"query".equals(parameter.path("in").asText()) || !parameter.path("required").asBoolean(false)) {
                continue;
            }
            String name = parameter.path("name").asText();
            String value = sampleValue(parameter.path("schema"), specification);
            if (!name.isBlank()) {
                pairs.add(name + "=" + value);
            }
        }
    }

    private String buildRequestBody(JsonNode operation, JsonNode root) {
        JsonNode content = operation.path("requestBody")
                .path("content");
        if (!content.isObject()) {
            return null;
        }

        JsonNode mediaType = firstAvailable(content, List.of(
                "application/json",
                "application/*+json",
                "application/x-www-form-urlencoded"
        ));
        JsonNode schema = mediaType.path("schema");
        if (schema.isMissingNode()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(sampleJsonValue(schema, root, 0));
        } catch (Exception e) {
            return "{}";
        }
    }

    private JsonNode firstAvailable(JsonNode content, List<String> preferredNames) {
        for (String name : preferredNames) {
            JsonNode node = content.path(name);
            if (!node.isMissingNode()) {
                return node;
            }
        }
        Iterator<JsonNode> values = content.elements();
        return values.hasNext() ? values.next() : MissingNode.getInstance();
    }

    private Object sampleJsonValue(JsonNode schema, JsonNode root, int depth) {
        if (depth > 4) {
            return "test";
        }

        JsonNode effectiveSchema = firstSchema(resolveRef(schema, root));
        String type = effectiveSchema.path("type").asText("object");

        if ("array".equals(type)) {
            return List.of(sampleJsonValue(effectiveSchema.path("items"), root, depth + 1));
        }
        if ("object".equals(type) || effectiveSchema.has("properties")) {
            return sampleObject(effectiveSchema, root, depth + 1);
        }
        if (effectiveSchema.has("example")) {
            return objectMapper.convertValue(effectiveSchema.path("example"), Object.class);
        }
        if (effectiveSchema.has("default")) {
            return objectMapper.convertValue(effectiveSchema.path("default"), Object.class);
        }

        return DEFAULT_VALUES.getOrDefault(type, "test");
    }

    private Map<String, Object> sampleObject(JsonNode schema, JsonNode root, int depth) {
        JsonNode properties = schema.path("properties");
        if (!properties.isObject()) {
            return Map.of();
        }

        java.util.LinkedHashMap<String, Object> value = new java.util.LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        schema.path("required").forEach(requiredNode -> required.add(requiredNode.asText()));

        Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (required.isEmpty() || required.contains(field.getKey())) {
                value.put(field.getKey(), sampleJsonValue(field.getValue(), root, depth + 1));
            }
        }
        return value;
    }

    private JsonNode firstSchema(JsonNode schema) {
        for (String key : List.of("oneOf", "anyOf", "allOf")) {
            JsonNode variants = schema.path(key);
            if (variants.isArray() && !variants.isEmpty()) {
                return variants.get(0);
            }
        }
        return schema;
    }

    private JsonNode resolveRef(JsonNode schema, JsonNode root) {
        String ref = schema.path("$ref").asText("");
        if (ref.isBlank() || !ref.startsWith("#/")) {
            return schema;
        }

        JsonNode resolved = root.at(ref.substring(1));
        return resolved.isMissingNode() ? schema : resolved;
    }

    private String sampleValue(JsonNode schema, JsonNode root) {
        Object value = sampleJsonValue(schema, root, 0);
        return String.valueOf(value);
    }

    private String buildName(String method, String path, JsonNode operation) {
        String summary = operation.path("summary").asText("");
        if (!summary.isBlank()) {
            return method + " " + path + " - " + summary;
        }
        return method + " " + path;
    }

    private String buildTestKey(String method, String path, int index) {
        String normalizedPath = path.replaceAll("[^A-Za-z0-9]+", "_")
                .replaceAll("^_|_$", "")
                .toUpperCase(Locale.ROOT);
        return "OPENAPI_" + method + "_" + normalizedPath + "_" + (index + 1);
    }

    private String firstText(JsonNode array, String fallback) {
        if (array.isArray() && !array.isEmpty()) {
            String value = array.get(0).asText();
            if (!value.isBlank()) {
                return value;
            }
        }
        return fallback;
    }

    private String textOrFallback(JsonNode primary, JsonNode secondary, String fallback) {
        if (!primary.asText("").isBlank()) {
            return primary.asText();
        }
        if (!secondary.asText("").isBlank()) {
            return secondary.asText();
        }
        return fallback;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private ProjectResponse toProjectResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getBaseUrl(),
                project.getDescription()
        );
    }

    private ApiTestResponse toApiTestResponse(ApiTest apiTest) {
        return new ApiTestResponse(
                apiTest.getId(),
                apiTest.getName(),
                apiTest.getDescription(),
                apiTest.getTestKey(),
                apiTest.getFeature(),
                apiTest.getStory(),
                apiTest.getMethod(),
                apiTest.getEndpoint(),
                apiTest.getRequestBody(),
                apiTest.getRequestHeadersJson(),
                apiTest.getRunOrder(),
                apiTest.getCaptureJsonPath(),
                apiTest.getCaptureVariableName(),
                apiTest.getExpectedResponseBody(),
                apiTest.getExpectedJsonPath(),
                apiTest.getExpectedJsonValue(),
                apiTest.getExpectedHeaderName(),
                apiTest.getExpectedHeaderValue(),
                apiTest.getMaxResponseTimeMs(),
                apiTest.getExpectedStatus(),
                apiTest.getProject().getId()
        );
    }
}
