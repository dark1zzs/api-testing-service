package com.example.apitestingservice.tests;

import com.example.apitestingservice.model.ExecutionResult;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.springframework.stereotype.Component;

@Component
public class ApiTestExecutor {

    public ExecutionResult execute(String baseUrl, String endpoint, int expectedStatus) {

        try {
            String url = baseUrl + endpoint;

            Response response = RestAssured
                    .given()
                    .when()
                    .get(url);

            int actualStatus = response.getStatusCode();

            boolean success = actualStatus == expectedStatus;

            return new ExecutionResult(
                    success,
                    actualStatus,
                    success ? null : "Expected " + expectedStatus + ", but got " + actualStatus
            );

        } catch (Exception e) {
            return new ExecutionResult(
                    false,
                    0,
                    e.getMessage()
            );
        }
    }
}