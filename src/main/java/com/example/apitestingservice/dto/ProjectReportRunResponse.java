package com.example.apitestingservice.dto;

import java.time.LocalDateTime;

public record ProjectReportRunResponse(
        LocalDateTime startedAt,
        long testsCount,
        long passedCount,
        long failedCount,
        long totalDurationMs
) {
}
