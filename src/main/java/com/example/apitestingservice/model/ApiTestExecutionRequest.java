package com.example.apitestingservice.model;

public record ApiTestExecutionRequest(
        String baseUrl,
        String endpoint,
        String method,
        String requestBody,
        String expectedResponseBody,
        String expectedJsonPath,
        String expectedJsonValue,
        String expectedHeaderName,
        String expectedHeaderValue,
        Long maxResponseTimeMs,
        Integer expectedStatus
) {
}
