package com.example.apitestingservice.model;

import lombok.Getter;

@Getter
public class ExecutionResult {

    private final boolean success;
    private final int statusCode;
    private final long responseTimeMs;
    private final String responseBody;
    private final String errorMessage;

    public ExecutionResult(
            boolean success,
            int statusCode,
            long responseTimeMs,
            String responseBody,
            String errorMessage
    ) {
        this.success = success;
        this.statusCode = statusCode;
        this.responseTimeMs = responseTimeMs;
        this.responseBody = responseBody;
        this.errorMessage = errorMessage;
    }
}
