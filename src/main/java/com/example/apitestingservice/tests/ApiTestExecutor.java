package com.example.apitestingservice.tests;

import com.example.apitestingservice.model.ApiTestExecutionRequest;
import com.example.apitestingservice.model.ExecutionResult;
import io.restassured.path.json.JsonPath;
import org.springframework.http.HttpHeaders;
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

    public ExecutionResult execute(ApiTestExecutionRequest executionRequest) {
        long startTime = System.nanoTime();

        try {
            validateTestParameters(executionRequest);

            URI uri = buildUri(executionRequest.baseUrl(), executionRequest.endpoint());
            HttpMethod httpMethod = HttpMethod.valueOf(executionRequest.method().toUpperCase());

            RestClient.RequestBodySpec requestSpec = restClient
                    .method(httpMethod)
                    .uri(uri);

            RestClient.RequestHeadersSpec<?> requestHeadersSpec = hasRequestBody(executionRequest.requestBody())
                    ? requestSpec.contentType(MediaType.APPLICATION_JSON).body(executionRequest.requestBody())
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
            long responseTimeMs = calculateElapsedTimeMs(startTime);

            if (response == null) {
                return new ExecutionResult(false, 0, responseTimeMs, null, "Response was not received");
            }

            int actualStatus = response.getStatusCode().value();
            String actualResponseBody = response.getBody();
            String errorMessage = buildErrorMessage(
                    executionRequest.expectedStatus(),
                    actualStatus,
                    executionRequest.expectedResponseBody(),
                    executionRequest.expectedJsonPath(),
                    executionRequest.expectedJsonValue(),
                    executionRequest.expectedHeaderName(),
                    executionRequest.expectedHeaderValue(),
                    executionRequest.maxResponseTimeMs(),
                    responseTimeMs,
                    response.getHeaders(),
                    actualResponseBody
            );
            boolean success = errorMessage == null;

            return new ExecutionResult(
                    success,
                    actualStatus,
                    responseTimeMs,
                    actualResponseBody,
                    errorMessage
            );

        } catch (IllegalArgumentException e) {
            return new ExecutionResult(
                    false,
                    0,
                    calculateElapsedTimeMs(startTime),
                    null,
                    e.getMessage()
            );
        } catch (RestClientException e) {
            return new ExecutionResult(
                    false,
                    0,
                    calculateElapsedTimeMs(startTime),
                    null,
                    "Request execution failed: " + e.getMessage()
            );
        } catch (Exception e) {
            return new ExecutionResult(
                    false,
                    0,
                    calculateElapsedTimeMs(startTime),
                    null,
                    "Unexpected execution error: " + e.getMessage()
            );
        }
    }

    private void validateTestParameters(ApiTestExecutionRequest executionRequest) {
        if (executionRequest.baseUrl() == null || executionRequest.baseUrl().isBlank()) {
            throw new IllegalArgumentException("Base URL must not be empty");
        }

        if (executionRequest.endpoint() == null || executionRequest.endpoint().isBlank()) {
            throw new IllegalArgumentException("Endpoint must not be empty");
        }

        if (executionRequest.method() == null || executionRequest.method().isBlank()) {
            throw new IllegalArgumentException("HTTP method must not be empty");
        }

        if (!SUPPORTED_METHODS.contains(executionRequest.method().toUpperCase())) {
            throw new IllegalArgumentException("Unsupported HTTP method: " + executionRequest.method());
        }

        if (executionRequest.expectedStatus() == null) {
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
            String expectedHeaderName,
            String expectedHeaderValue,
            Long maxResponseTimeMs,
            long responseTimeMs,
            HttpHeaders actualHeaders,
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

        if (hasHeaderExpectation(expectedHeaderName, expectedHeaderValue)) {
            String headerError = validateHeader(actualHeaders, expectedHeaderName, expectedHeaderValue);
            if (headerError != null) {
                return headerError;
            }
        }

        if (hasMaxResponseTime(maxResponseTimeMs) && responseTimeMs > maxResponseTimeMs) {
            return "Expected response time <= " + maxResponseTimeMs
                    + " ms, but got " + responseTimeMs + " ms";
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

    private boolean hasHeaderExpectation(String expectedHeaderName, String expectedHeaderValue) {
        return expectedHeaderName != null
                && !expectedHeaderName.isBlank()
                && expectedHeaderValue != null
                && !expectedHeaderValue.isBlank();
    }

    private String validateHeader(
            HttpHeaders actualHeaders,
            String expectedHeaderName,
            String expectedHeaderValue
    ) {
        String actualHeaderValue = actualHeaders.getFirst(expectedHeaderName);

        if (actualHeaderValue == null) {
            return "Header " + expectedHeaderName + " was not found in response";
        }

        if (!actualHeaderValue.contains(expectedHeaderValue)) {
            return "Expected header " + expectedHeaderName + " to contain "
                    + expectedHeaderValue + ", but got " + actualHeaderValue;
        }

        return null;
    }

    private boolean hasMaxResponseTime(Long maxResponseTimeMs) {
        return maxResponseTimeMs != null && maxResponseTimeMs > 0;
    }

    private long calculateElapsedTimeMs(long startTime) {
        return (System.nanoTime() - startTime) / 1_000_000;
    }
}
