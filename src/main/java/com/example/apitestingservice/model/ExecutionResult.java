package com.example.apitestingservice.model;

public class ExecutionResult {

    private boolean success;
    private int statusCode;
    private long responseTimeMs;
    private String responseBody;
    private String errorMessage;

    public ExecutionResult() {
    }

    public ExecutionResult(boolean success, int statusCode, String errorMessage) {
        this(success, statusCode, null, errorMessage);
    }

    public ExecutionResult(boolean success, int statusCode, String responseBody, String errorMessage) {
        this(success, statusCode, 0, responseBody, errorMessage);
    }

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

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
