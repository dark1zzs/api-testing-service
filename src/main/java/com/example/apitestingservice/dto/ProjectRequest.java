package com.example.apitestingservice.dto;

import jakarta.validation.constraints.NotBlank;

public record ProjectRequest(
        @NotBlank String name,
        @NotBlank String baseUrl,
        String description
) {
}
