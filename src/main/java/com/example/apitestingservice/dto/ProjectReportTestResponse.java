package com.example.apitestingservice.dto;

import java.time.LocalDateTime;

public record ProjectReportTestResponse(
        Long testId,
        String testName,
        String testKey,
        boolean success,
        Integer statusCode,
        Long responseTimeMs,
        String errorMessage,
        LocalDateTime lastRunAt
) {
}
