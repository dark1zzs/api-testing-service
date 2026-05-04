package com.example.apitestingservice.tests;

import com.example.apitestingservice.model.ExecutionResult;
import io.restassured.path.json.JsonPath;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@Component
public class ApiTestExecutor {

    private static final Set<String> SUPPORTED_METHODS = Set.of("GET", "POST", "PUT", "DELETE");

    private final RestClient restClient;

    public ApiTestExecutor(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public ExecutionResult execute(String baseUrl, String endpoint, String method, Integer expectedStatus) {
        return execute(baseUrl, endpoint, method, null, expectedStatus);
    }

    public ExecutionResult execute(
            String baseUrl,
            String endpoint,
            String method,
            String requestBody,
            Integer expectedStatus
    ) {
        return execute(baseUrl, endpoint, method, requestBody, null, expectedStatus);
    }

    public ExecutionResult execute(
            String baseUrl,
            String endpoint,
            String method,
            String requestBody,
            String expectedResponseBody,
            Integer expectedStatus
    ) {
        return execute(baseUrl, endpoint, method, requestBody, expectedResponseBody, null, null, expectedStatus);
    }

    public ExecutionResult execute(
            String baseUrl,
            String endpoint,
            String method,
            String requestBody,
            String expectedResponseBody,
            String expectedJsonPath,
            String expectedJsonValue,
            Integer expectedStatus
    ) {

        try {
            validateTestParameters(baseUrl, endpoint, method, expectedStatus);

            URI uri = buildUri(baseUrl, endpoint);
            HttpMethod httpMethod = HttpMethod.valueOf(method.toUpperCase());

            RestClient.RequestBodySpec requestSpec = restClient
                    .method(httpMethod)
                    .uri(uri);

            RestClient.RequestHeadersSpec<?> requestHeadersSpec = hasRequestBody(requestBody)
                    ? requestSpec.contentType(MediaType.APPLICATION_JSON).body(requestBody)
                    : requestSpec;

            ResponseEntity<String> response = requestHeadersSpec.exchange((request, clientResponse) -> {
                String responseBody = new String(
                        clientResponse.getBody().readAllBytes(),
                        StandardCharsets.UTF_8
                );

                return ResponseEntity
                        .status(clientResponse.getStatusCode())
                        .headers(clientResponse.getHeaders())
                        .body(responseBody);
            });

            if (response == null) {
                return new ExecutionResult(false, 0, "Response was not received");
            }

            int actualStatus = response.getStatusCode().value();
            String actualResponseBody = response.getBody();
            String errorMessage = buildErrorMessage(
                    expectedStatus,
                    actualStatus,
                    expectedResponseBody,
                    expectedJsonPath,
                    expectedJsonValue,
                    actualResponseBody
            );
            boolean success = errorMessage == null;

            return new ExecutionResult(
                    success,
                    actualStatus,
                    actualResponseBody,
                    errorMessage
            );

        } catch (IllegalArgumentException e) {
            return new ExecutionResult(
                    false,
                    0,
                    e.getMessage()
            );
        } catch (RestClientException e) {
            return new ExecutionResult(
                    false,
                    0,
                    "Request execution failed: " + e.getMessage()
            );
        } catch (Exception e) {
            return new ExecutionResult(
                    false,
                    0,
                    "Unexpected execution error: " + e.getMessage()
            );
        }
    }

    private void validateTestParameters(String baseUrl, String endpoint, String method, Integer expectedStatus) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("Base URL must not be empty");
        }

        if (endpoint == null || endpoint.isBlank()) {
            throw new IllegalArgumentException("Endpoint must not be empty");
        }

        if (method == null || method.isBlank()) {
            throw new IllegalArgumentException("HTTP method must not be empty");
        }

        if (!SUPPORTED_METHODS.contains(method.toUpperCase())) {
            throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        }

        if (expectedStatus == null) {
            throw new IllegalArgumentException("Expected status must not be empty");
        }
    }

    private URI buildUri(String baseUrl, String endpoint) {
        String normalizedBaseUrl = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;

        String normalizedEndpoint = endpoint.startsWith("/")
                ? endpoint
                : "/" + endpoint;

        return URI.create(normalizedBaseUrl + normalizedEndpoint);
    }

    private boolean hasRequestBody(String requestBody) {
        return requestBody != null && !requestBody.isBlank();
    }

    private String buildErrorMessage(
            int expectedStatus,
            int actualStatus,
            String expectedResponseBody,
            String expectedJsonPath,
            String expectedJsonValue,
            String actualResponseBody
    ) {
        if (actualStatus != expectedStatus) {
            return "Expected " + expectedStatus + ", but got " + actualStatus;
        }

        if (hasExpectedResponseBody(expectedResponseBody)
                && !containsExpectedBody(actualResponseBody, expectedResponseBody)) {
            return "Response body does not contain expected content";
        }

        if (hasJsonPathExpectation(expectedJsonPath, expectedJsonValue)) {
            String jsonPathError = validateJsonPath(actualResponseBody, expectedJsonPath, expectedJsonValue);
            if (jsonPathError != null) {
                return jsonPathError;
            }
        }

        return null;
    }

    private boolean hasExpectedResponseBody(String expectedResponseBody) {
        return expectedResponseBody != null && !expectedResponseBody.isBlank();
    }

    private boolean containsExpectedBody(String actualResponseBody, String expectedResponseBody) {
        return actualResponseBody != null && actualResponseBody.contains(expectedResponseBody);
    }

    private boolean hasJsonPathExpectation(String expectedJsonPath, String expectedJsonValue) {
        return expectedJsonPath != null
                && !expectedJsonPath.isBlank()
                && expectedJsonValue != null
                && !expectedJsonValue.isBlank();
    }

    private String validateJsonPath(String actualResponseBody, String expectedJsonPath, String expectedJsonValue) {
        if (actualResponseBody == null || actualResponseBody.isBlank()) {
            return "Response body is empty, JSONPath cannot be checked";
        }

        try {
            Object actualValue = JsonPath.from(actualResponseBody).get(normalizeJsonPath(expectedJsonPath));

            if (actualValue == null) {
                return "JSONPath " + expectedJsonPath + " was not found in response body";
            }

            if (!expectedJsonValue.equals(String.valueOf(actualValue))) {
                return "Expected JSONPath " + expectedJsonPath + " to be "
                        + expectedJsonValue + ", but got " + actualValue;
            }

            return null;
        } catch (Exception e) {
            return "Response body is not valid JSON or JSONPath is invalid";
        }
    }

    private String normalizeJsonPath(String jsonPath) {
        String trimmedPath = jsonPath.trim();

        if (trimmedPath.startsWith("$.")) {
            return trimmedPath.substring(2);
        }

        if (trimmedPath.equals("$")) {
            return "";
        }

        return trimmedPath;
    }
}
