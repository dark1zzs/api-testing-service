package com.example.apitestingservice.dto;

public record ProjectResponse(
        Long id,
        String name,
        String baseUrl,
        String description
) {
}
