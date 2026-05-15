package com.example.apitestingservice.dto;

import java.util.List;

public record OpenApiGenerationResponse(
        ProjectResponse project,
        int generatedTestsCount,
        List<ApiTestResponse> tests
) {
}
