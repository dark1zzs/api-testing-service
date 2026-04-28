package com.example.apitestingservice.model;

import java.time.LocalDateTime;

public record TestExecutionResponse(
        Long testId,
        String testName,
        boolean success,
        int statusCode,
        String errorMessage,
        LocalDateTime executedAt
) {
}
