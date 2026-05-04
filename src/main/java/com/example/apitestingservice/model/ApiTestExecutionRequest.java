package com.example.apitestingservice.model;

import java.util.Map;

public record ApiTestExecutionRequest(
        String baseUrl,
        String endpoint,
        String method,
        String requestBody,
        Map<String, String> requestHeaders,
        String expectedResponseBody,
        String expectedJsonPath,
        String expectedJsonValue,
        String expectedHeaderName,
        String expectedHeaderValue,
        Long maxResponseTimeMs,
        Integer expectedStatus
) {
}
