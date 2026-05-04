package com.example.apitestingservice.tests;

import com.example.apitestingservice.model.ApiTestExecutionRequest;
import com.example.apitestingservice.model.ExecutionResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiTestExecutorTest {

    private HttpServer server;
    private ApiTestExecutor executor;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        executor = new ApiTestExecutor(RestClient.builder());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldTreatExpectedClientErrorStatusAsSuccessfulResult() {
        server.createContext("/missing", exchange -> send(exchange, 404, "not found"));
        server.start();

        ExecutionResult result = executor.execute(request("/missing", "GET", 404));

        assertTrue(result.isSuccess());
        assertEquals(404, result.getStatusCode());
        assertNull(result.getErrorMessage());
    }

    @Test
    void shouldUseConfiguredHttpMethod() {
        server.createContext("/created", exchange -> {
            int status = "POST".equals(exchange.getRequestMethod()) ? 201 : 405;
            send(exchange, status, "");
        });
        server.start();

        ExecutionResult result = executor.execute(request("/created", "POST", 201));

        assertTrue(result.isSuccess());
        assertEquals(201, result.getStatusCode());
    }

    @Test
    void shouldSendConfiguredRequestBody() {
        server.createContext("/echo", exchange -> {
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            int status = "{\"name\":\"demo\"}".equals(requestBody) ? 201 : 400;
            send(exchange, status, "");
        });
        server.start();

        ExecutionResult result = executor.execute(
                request("/echo", "POST", 201, "{\"name\":\"demo\"}")
        );

        assertTrue(result.isSuccess());
        assertEquals(201, result.getStatusCode());
    }

    @Test
    void shouldTreatExpectedResponseBodyContentAsSuccessfulResult() {
        server.createContext("/profile", exchange -> send(exchange, 200, "{\"name\":\"demo\"}"));
        server.start();

        ExecutionResult result = executor.execute(
                profileRequestWithExpectedResponseBody("\"name\":\"demo\"")
        );

        assertTrue(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertEquals("{\"name\":\"demo\"}", result.getResponseBody());
        assertNull(result.getErrorMessage());
    }

    @Test
    void shouldFailWhenResponseBodyDoesNotContainExpectedContent() {
        server.createContext("/profile", exchange -> send(exchange, 200, "{\"name\":\"demo\"}"));
        server.start();

        ExecutionResult result = executor.execute(
                profileRequestWithExpectedResponseBody("\"name\":\"admin\"")
        );

        assertFalse(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertEquals("Response body does not contain expected content", result.getErrorMessage());
    }

    @Test
    void shouldTreatExpectedJsonPathValueAsSuccessfulResult() {
        server.createContext("/post", exchange -> send(exchange, 200, "{\"userId\":1,\"title\":\"demo\"}"));
        server.start();

        ExecutionResult result = executor.execute(
                postRequestWithExpectedUserId("1")
        );

        assertTrue(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertNull(result.getErrorMessage());
    }

    @Test
    void shouldFailWhenJsonPathValueDoesNotMatch() {
        server.createContext("/post", exchange -> send(exchange, 200, "{\"userId\":1,\"title\":\"demo\"}"));
        server.start();

        ExecutionResult result = executor.execute(
                postRequestWithExpectedUserId("2")
        );

        assertFalse(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertEquals("Expected JSONPath $.userId to be 2, but got 1", result.getErrorMessage());
    }

    @Test
    void shouldTreatExpectedResponseHeaderAsSuccessfulResult() {
        server.createContext("/headers", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "application/json;charset=UTF-8");
            send(exchange, 200, "{}");
        });
        server.start();

        ExecutionResult result = executor.execute(
                headersRequestWithExpectedJsonContentType()
        );

        assertTrue(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertNull(result.getErrorMessage());
    }

    @Test
    void shouldFailWhenResponseHeaderDoesNotContainExpectedValue() {
        server.createContext("/headers", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            send(exchange, 200, "{}");
        });
        server.start();

        ExecutionResult result = executor.execute(
                headersRequestWithExpectedJsonContentType()
        );

        assertFalse(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertEquals(
                "Expected header Content-Type to contain application/json, but got text/plain",
                result.getErrorMessage()
        );
    }

    @Test
    void shouldTreatResponseWithinExpectedTimeAsSuccessfulResult() {
        server.createContext("/fast", exchange -> send(exchange, 200, "{}"));
        server.start();

        ExecutionResult result = executor.execute(
                requestWithMaxResponseTime("/fast", 5_000L)
        );

        assertTrue(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertTrue(result.getResponseTimeMs() >= 0);
        assertNull(result.getErrorMessage());
    }

    @Test
    void shouldFailWhenResponseExceedsExpectedTime() {
        server.createContext("/slow", exchange -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            send(exchange, 200, "{}");
        });
        server.start();

        ExecutionResult result = executor.execute(
                requestWithMaxResponseTime("/slow", 1L)
        );

        assertFalse(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertTrue(result.getResponseTimeMs() >= 1);
        assertTrue(result.getErrorMessage().startsWith("Expected response time <= 1 ms, but got "));
    }

    @Test
    void shouldReturnFailedResultForInvalidMethod() {
        server.start();

        ExecutionResult result = executor.execute(request("/test", "BREW", 200));

        assertFalse(result.isSuccess());
        assertEquals(0, result.getStatusCode());
        assertTrue(result.getErrorMessage().contains("BREW"));
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private ApiTestExecutionRequest request(String endpoint, String method, Integer expectedStatus) {
        return request(endpoint, method, expectedStatus, null);
    }

    private ApiTestExecutionRequest request(
            String endpoint,
            String method,
            Integer expectedStatus,
            String requestBody
    ) {
        return new ApiTestExecutionRequest(
                baseUrl(),
                endpoint,
                method,
                requestBody,
                null,
                null,
                null,
                null,
                null,
                null,
                expectedStatus
        );
    }

    private ApiTestExecutionRequest profileRequestWithExpectedResponseBody(
            String expectedResponseBody
    ) {
        return new ApiTestExecutionRequest(
                baseUrl(),
                "/profile",
                "GET",
                null,
                expectedResponseBody,
                null,
                null,
                null,
                null,
                null,
                200
        );
    }

    private ApiTestExecutionRequest postRequestWithExpectedUserId(
            String expectedJsonValue
    ) {
        return new ApiTestExecutionRequest(
                baseUrl(),
                "/post",
                "GET",
                null,
                null,
                "$.userId",
                expectedJsonValue,
                null,
                null,
                null,
                200
        );
    }

    private ApiTestExecutionRequest headersRequestWithExpectedJsonContentType() {
        return new ApiTestExecutionRequest(
                baseUrl(),
                "/headers",
                "GET",
                null,
                null,
                null,
                null,
                "Content-Type",
                "application/json",
                null,
                200
        );
    }

    private ApiTestExecutionRequest requestWithMaxResponseTime(
            String endpoint,
            Long maxResponseTimeMs
    ) {
        return new ApiTestExecutionRequest(
                baseUrl(),
                endpoint,
                "GET",
                null,
                null,
                null,
                null,
                null,
                null,
                maxResponseTimeMs,
                200
        );
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(status, responseBody.length);
        exchange.getResponseBody().write(responseBody);
        exchange.close();
    }
}
