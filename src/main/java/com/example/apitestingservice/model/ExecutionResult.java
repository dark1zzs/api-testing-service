package com.example.apitestingservice.model;

public class ExecutionResult {

    private boolean success;
    private int statusCode;
    private String responseBody;
    private String errorMessage;

    public ExecutionResult() {
    }

    public ExecutionResult(boolean success, int statusCode, String errorMessage) {
        this(success, statusCode, null, errorMessage);
    }

    public ExecutionResult(boolean success, int statusCode, String responseBody, String errorMessage) {
        this.success = success;
        this.statusCode = statusCode;
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
