package com.example.apitestingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record ApiTestRequest(
        @NotBlank String name,
        String description,
        String testKey,
        @NotBlank
        @Pattern(regexp = "GET|POST|PUT|DELETE", message = "Method must be one of: GET, POST, PUT, DELETE")
        String method,
        @NotBlank String endpoint,
        String requestBody,
        String expectedResponseBody,
        String expectedJsonPath,
        String expectedJsonValue,
        String expectedHeaderName,
        String expectedHeaderValue,
        Long maxResponseTimeMs,
        @NotNull Integer expectedStatus,
        Integer runOrder,
        String requestHeadersJson,
        String captureJsonPath,
        String captureVariableName
) {
}
