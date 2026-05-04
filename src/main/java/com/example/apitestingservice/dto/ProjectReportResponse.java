package com.example.apitestingservice.dto;

import java.time.LocalDateTime;

public record ProjectReportResponse(
        Long projectId,
        String projectName,
        long totalTests,
        long passedTests,
        long failedTests,
        long notRunTests,
        double successRate,
        LocalDateTime lastRunAt
) {
}
