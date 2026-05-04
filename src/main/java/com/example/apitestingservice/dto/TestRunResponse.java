package com.example.apitestingservice.dto;

import java.time.LocalDateTime;

public record TestRunResponse(
        Long id,
        Long testId,
        String testName,
        boolean success,
        int statusCode,
        Long responseTimeMs,
        String responseBody,
        String errorMessage,
        LocalDateTime executedAt
) {
}
