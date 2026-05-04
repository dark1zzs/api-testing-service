package com.example.apitestingservice.service;

import com.example.apitestingservice.dto.ApiTestRequest;
import com.example.apitestingservice.dto.ApiTestResponse;
import com.example.apitestingservice.entity.ApiTest;
import com.example.apitestingservice.entity.Project;
import com.example.apitestingservice.exception.NotFoundException;
import com.example.apitestingservice.repository.ApiTestRepository;
import com.example.apitestingservice.repository.ProjectRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiTestService {

    private final ApiTestRepository apiTestRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    public ApiTestService(
            ApiTestRepository apiTestRepository,
            ProjectRepository projectRepository,
            ObjectMapper objectMapper
    ) {
        this.apiTestRepository = apiTestRepository;
        this.projectRepository = projectRepository;
        this.objectMapper = objectMapper;
    }

    public ApiTestResponse createApiTest(Long projectId, ApiTestRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        validateRequest(request);

        ApiTest apiTest = new ApiTest();
        updateApiTestFields(apiTest, request);
        apiTest.setProject(project);

        return toResponse(apiTestRepository.save(apiTest));
    }

    public List<ApiTestResponse> getTestsByProjectId(Long projectId) {
        ensureProjectExists(projectId);

        return apiTestRepository.findByProjectIdOrderByRunOrderAscIdAsc(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ApiTestResponse getApiTest(Long projectId, Long testId) {
        return toResponse(findApiTestInProject(projectId, testId));
    }

    public ApiTestResponse updateApiTest(Long projectId, Long testId, ApiTestRequest request) {
        ApiTest apiTest = findApiTestInProject(projectId, testId);
        validateRequest(request);
        updateApiTestFields(apiTest, request);

        return toResponse(apiTestRepository.save(apiTest));
    }

    public void deleteApiTest(Long projectId, Long testId) {
        ApiTest apiTest = findApiTestInProject(projectId, testId);
        apiTestRepository.delete(apiTest);
    }

    private void validateRequest(ApiTestRequest request) {
        boolean hasPath = request.captureJsonPath() != null && !request.captureJsonPath().isBlank();
        boolean hasVar = request.captureVariableName() != null && !request.captureVariableName().isBlank();
        if (hasPath != hasVar) {
            throw new IllegalArgumentException(
                    "captureJsonPath and captureVariableName must both be set or both omitted"
            );
        }
        if (hasVar && !request.captureVariableName().matches("[A-Za-z_]\\w*")) {
            throw new IllegalArgumentException(
                    "captureVariableName must start with letter or underscore and contain only letters, digits, underscore"
            );
        }

        validateHeadersJson(request.requestHeadersJson());

        if (request.runOrder() != null && request.runOrder() < 0) {
            throw new IllegalArgumentException("runOrder must be >= 0");
        }
    }

    private void validateHeadersJson(String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw new IllegalArgumentException("requestHeadersJson must be a JSON object");
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("requestHeadersJson is not valid JSON");
        }
    }

    private void ensureProjectExists(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new NotFoundException("Project not found");
        }
    }

    private ApiTest findApiTestInProject(Long projectId, Long testId) {
        ensureProjectExists(projectId);
        return apiTestRepository.findByIdAndProjectId(testId, projectId)
                .orElseThrow(() -> new NotFoundException("Test not found in project"));
    }

    private void updateApiTestFields(ApiTest apiTest, ApiTestRequest request) {
        apiTest.setName(request.name());
        apiTest.setDescription(request.description());
        apiTest.setTestKey(request.testKey());
        apiTest.setMethod(request.method());
        apiTest.setEndpoint(request.endpoint());
        apiTest.setRequestBody(request.requestBody());
        apiTest.setRequestHeadersJson(blankToNull(request.requestHeadersJson()));
        apiTest.setRunOrder(request.runOrder() != null ? request.runOrder() : 0);
        apiTest.setCaptureJsonPath(blankToNull(request.captureJsonPath()));
        apiTest.setCaptureVariableName(blankToNull(request.captureVariableName()));
        apiTest.setExpectedResponseBody(request.expectedResponseBody());
        apiTest.setExpectedJsonPath(request.expectedJsonPath());
        apiTest.setExpectedJsonValue(request.expectedJsonValue());
        apiTest.setExpectedHeaderName(request.expectedHeaderName());
        apiTest.setExpectedHeaderValue(request.expectedHeaderValue());
        apiTest.setMaxResponseTimeMs(request.maxResponseTimeMs());
        apiTest.setExpectedStatus(request.expectedStatus());
    }

    private static String blankToNull(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        return s;
    }

    private ApiTestResponse toResponse(ApiTest apiTest) {
        return new ApiTestResponse(
                apiTest.getId(),
                apiTest.getName(),
                apiTest.getDescription(),
                apiTest.getTestKey(),
                apiTest.getMethod(),
                apiTest.getEndpoint(),
                apiTest.getRequestBody(),
                apiTest.getRequestHeadersJson(),
                apiTest.getRunOrder(),
                apiTest.getCaptureJsonPath(),
                apiTest.getCaptureVariableName(),
                apiTest.getExpectedResponseBody(),
                apiTest.getExpectedJsonPath(),
                apiTest.getExpectedJsonValue(),
                apiTest.getExpectedHeaderName(),
                apiTest.getExpectedHeaderValue(),
                apiTest.getMaxResponseTimeMs(),
                apiTest.getExpectedStatus(),
                apiTest.getProject().getId()
        );
    }
}
