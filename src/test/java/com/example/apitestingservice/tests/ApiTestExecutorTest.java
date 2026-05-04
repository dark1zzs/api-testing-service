package com.example.apitestingservice.tests;

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

        ExecutionResult result = executor.execute(baseUrl(), "/missing", "GET", 404);

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

        ExecutionResult result = executor.execute(baseUrl(), "/created", "POST", 201);

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
                baseUrl(),
                "/echo",
                "POST",
                "{\"name\":\"demo\"}",
                201
        );

        assertTrue(result.isSuccess());
        assertEquals(201, result.getStatusCode());
    }

    @Test
    void shouldTreatExpectedResponseBodyContentAsSuccessfulResult() {
        server.createContext("/profile", exchange -> send(exchange, 200, "{\"name\":\"demo\"}"));
        server.start();

        ExecutionResult result = executor.execute(
                baseUrl(),
                "/profile",
                "GET",
                null,
                "\"name\":\"demo\"",
                200
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
                baseUrl(),
                "/profile",
                "GET",
                null,
                "\"name\":\"admin\"",
                200
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
                baseUrl(),
                "/post",
                "GET",
                null,
                null,
                "$.userId",
                "1",
                200
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
                baseUrl(),
                "/post",
                "GET",
                null,
                null,
                "$.userId",
                "2",
                200
        );

        assertFalse(result.isSuccess());
        assertEquals(200, result.getStatusCode());
        assertEquals("Expected JSONPath $.userId to be 2, but got 1", result.getErrorMessage());
    }

    @Test
    void shouldReturnFailedResultForInvalidMethod() {
        server.start();

        ExecutionResult result = executor.execute(baseUrl(), "/test", "BREW", 200);

        assertFalse(result.isSuccess());
        assertEquals(0, result.getStatusCode());
        assertTrue(result.getErrorMessage().contains("BREW"));
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    private void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] responseBody = body.getBytes(StandardCharsets.UTF_8);

        exchange.sendResponseHeaders(status, responseBody.length);
        exchange.getResponseBody().write(responseBody);
        exchange.close();
    }
}
