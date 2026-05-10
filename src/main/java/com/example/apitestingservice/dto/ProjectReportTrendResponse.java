package com.example.apitestingservice.dto;

import java.time.LocalDate;

public record ProjectReportTrendResponse(
        LocalDate date,
        long runsCount,
        long passedCount,
        long failedCount,
        long totalDurationMs
) {
}
