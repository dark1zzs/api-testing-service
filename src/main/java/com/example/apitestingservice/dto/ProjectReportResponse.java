package com.example.apitestingservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ProjectReportResponse(
        Long projectId,
        String projectName,
        long totalTests,
        long passedTests,
        long failedTests,
        long notRunTests,
        double successRate,
        LocalDateTime lastRunAt,
        List<ProjectReportTestResponse> tests
) {
}
