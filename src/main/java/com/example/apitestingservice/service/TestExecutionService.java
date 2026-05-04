package com.example.apitestingservice.service;

import com.example.apitestingservice.dto.TestRunResponse;
import com.example.apitestingservice.entity.ApiTest;
import com.example.apitestingservice.entity.TestRun;
import com.example.apitestingservice.exception.NotFoundException;
import com.example.apitestingservice.model.ApiTestExecutionRequest;
import com.example.apitestingservice.model.ExecutionResult;
import com.example.apitestingservice.model.TestExecutionResponse;
import com.example.apitestingservice.repository.ApiTestRepository;
import com.example.apitestingservice.repository.ProjectRepository;
import com.example.apitestingservice.repository.TestRunRepository;
import com.example.apitestingservice.tests.ApiTestExecutor;
import com.example.apitestingservice.util.JsonPaths;
import com.example.apitestingservice.util.VariableInterpolator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TestExecutionService {

    private final ApiTestRepository apiTestRepository;
    private final ProjectRepository projectRepository;
    private final TestRunRepository testRunRepository;
    private final ApiTestExecutor apiTestExecutor;
    private final ObjectMapper objectMapper;

    public TestExecutionService(ApiTestRepository apiTestRepository,
                                ProjectRepository projectRepository,
                                TestRunRepository testRunRepository,
                                ApiTestExecutor apiTestExecutor,
                                ObjectMapper objectMapper) {
        this.apiTestRepository = apiTestRepository;
        this.projectRepository = projectRepository;
        this.testRunRepository = testRunRepository;
        this.apiTestExecutor = apiTestExecutor;
        this.objectMapper = objectMapper;
    }

    public ExecutionResult runTest(Long testId) {
        ApiTest test = apiTestRepository.findById(testId)
                .orElseThrow(() -> new NotFoundException("Test not found"));

        ApiTestExecutionRequest request = buildExecutionRequest(test, Map.of());
        TestRun testRun = executeAndSave(test, request);

        return new ExecutionResult(
                testRun.isSuccess(),
                testRun.getStatusCode(),
                testRun.getResponseTimeMs(),
                testRun.getResponseBody(),
                testRun.getErrorMessage()
        );
    }

    public List<TestExecutionResponse> runProjectTests(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        List<ApiTest> tests = apiTestRepository.findByProjectIdOrderByRunOrderAscIdAsc(projectId);
        Map<String, String> variables = new LinkedHashMap<>();
        List<TestExecutionResponse> responses = new ArrayList<>();

        for (ApiTest test : tests) {
            ApiTestExecutionRequest request = buildExecutionRequest(test, variables);
            TestRun testRun = executeAndSave(test, request);
            responses.add(toResponse(testRun));

            if (testRun.isSuccess()) {
                applyCapture(test, testRun.getResponseBody(), variables);
            }
        }

        return responses;
    }

    public List<TestRunResponse> getTestHistory(Long testId) {
        apiTestRepository.findById(testId)
                .orElseThrow(() -> new NotFoundException("Test not found"));

        return testRunRepository.findByApiTestId(testId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private void applyCapture(ApiTest test, String responseBody, Map<String, String> variables) {
        String path = test.getCaptureJsonPath();
        String varName = test.getCaptureVariableName();
        if (path == null || path.isBlank() || varName == null || varName.isBlank()) {
            return;
        }

        String value = JsonPaths.readString(responseBody, path);
        if (value != null) {
            variables.put(varName, value);
        }
    }

    private ApiTestExecutionRequest buildExecutionRequest(ApiTest test, Map<String, String> variables) {
        String interpolatedBody = VariableInterpolator.interpolate(test.getRequestBody(), variables);
        Map<String, String> headerMap = parseRequestHeadersJson(test.getRequestHeadersJson());
        Map<String, String> resolvedHeaders = new LinkedHashMap<>();
        for (var e : headerMap.entrySet()) {
            resolvedHeaders.put(e.getKey(), VariableInterpolator.interpolate(e.getValue(), variables));
        }

        return new ApiTestExecutionRequest(
                test.getProject().getBaseUrl(),
                test.getEndpoint(),
                test.getMethod(),
                interpolatedBody,
                resolvedHeaders.isEmpty() ? null : resolvedHeaders,
                test.getExpectedResponseBody(),
                test.getExpectedJsonPath(),
                test.getExpectedJsonValue(),
                test.getExpectedHeaderName(),
                test.getExpectedHeaderValue(),
                test.getMaxResponseTimeMs(),
                test.getExpectedStatus()
        );
    }

    private Map<String, String> parseRequestHeadersJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw new IllegalArgumentException("requestHeadersJson must be a JSON object");
            }
            Map<String, String> out = new LinkedHashMap<>();
            root.fields().forEachRemaining(entry -> {
                JsonNode v = entry.getValue();
                if (v == null || v.isNull()) {
                    out.put(entry.getKey(), "");
                } else if (v.isTextual()) {
                    out.put(entry.getKey(), v.asText());
                } else {
                    out.put(entry.getKey(), v.toString());
                }
            });
            return out;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("requestHeadersJson is not valid JSON");
        }
    }

    private TestRun executeAndSave(ApiTest test, ApiTestExecutionRequest request) {
        ExecutionResult result = apiTestExecutor.execute(request);

        LocalDateTime executedAt = LocalDateTime.now();
        TestRun testRun = new TestRun();
        testRun.setApiTest(test);
        testRun.setSuccess(result.isSuccess());
        testRun.setStatusCode(result.getStatusCode());
        testRun.setResponseTimeMs(result.getResponseTimeMs());
        testRun.setResponseBody(result.getResponseBody());
        testRun.setErrorMessage(result.getErrorMessage());
        testRun.setExecutedAt(executedAt);

        return testRunRepository.save(testRun);
    }

    private TestExecutionResponse toResponse(TestRun testRun) {
        ApiTest test = testRun.getApiTest();

        return new TestExecutionResponse(
                test.getId(),
                test.getName(),
                testRun.isSuccess(),
                testRun.getStatusCode(),
                testRun.getResponseTimeMs(),
                testRun.getResponseBody(),
                testRun.getErrorMessage(),
                testRun.getExecutedAt()
        );
    }

    private TestRunResponse toHistoryResponse(TestRun testRun) {
        ApiTest test = testRun.getApiTest();

        return new TestRunResponse(
                testRun.getId(),
                test.getId(),
                test.getName(),
                testRun.isSuccess(),
                testRun.getStatusCode(),
                testRun.getResponseTimeMs(),
                testRun.getResponseBody(),
                testRun.getErrorMessage(),
                testRun.getExecutedAt()
        );
    }
}
