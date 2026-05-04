package com.example.apitestingservice.dto;

public record ApiTestResponse(
        Long id,
        String name,
        String description,
        String testKey,
        String method,
        String endpoint,
        String requestBody,
        String requestHeadersJson,
        Integer runOrder,
        String captureJsonPath,
        String captureVariableName,
        String expectedResponseBody,
        String expectedJsonPath,
        String expectedJsonValue,
        String expectedHeaderName,
        String expectedHeaderValue,
        Long maxResponseTimeMs,
        Integer expectedStatus,
        Long projectId
) {
}
