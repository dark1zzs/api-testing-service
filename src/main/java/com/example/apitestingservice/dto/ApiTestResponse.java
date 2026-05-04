package com.example.apitestingservice.dto;

public record ApiTestResponse(
        Long id,
        String name,
        String description,
        String testKey,
        String method,
        String endpoint,
        String requestBody,
        String expectedResponseBody,
        Integer expectedStatus,
        Long projectId
) {
}
