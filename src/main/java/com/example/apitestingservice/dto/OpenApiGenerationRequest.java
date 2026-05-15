package com.example.apitestingservice.dto;

import jakarta.validation.constraints.NotBlank;

public record OpenApiGenerationRequest(
        @NotBlank String projectName,
        @NotBlank String baseUrl,
        @NotBlank String openApiUrl,
        String description
) {
}
