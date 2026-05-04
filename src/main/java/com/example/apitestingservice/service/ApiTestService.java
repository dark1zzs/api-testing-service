package com.example.apitestingservice.service;

import com.example.apitestingservice.dto.ApiTestRequest;
import com.example.apitestingservice.dto.ApiTestResponse;
import com.example.apitestingservice.entity.ApiTest;
import com.example.apitestingservice.entity.Project;
import com.example.apitestingservice.exception.NotFoundException;
import com.example.apitestingservice.repository.ApiTestRepository;
import com.example.apitestingservice.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiTestService {

    private final ApiTestRepository apiTestRepository;
    private final ProjectRepository projectRepository;

    public ApiTestService(ApiTestRepository apiTestRepository, ProjectRepository projectRepository) {
        this.apiTestRepository = apiTestRepository;
        this.projectRepository = projectRepository;
    }

    public ApiTestResponse createApiTest(Long projectId, ApiTestRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        ApiTest apiTest = new ApiTest();
        updateApiTestFields(apiTest, request);
        apiTest.setProject(project);

        return toResponse(apiTestRepository.save(apiTest));
    }

    public List<ApiTestResponse> getTestsByProjectId(Long projectId) {
        ensureProjectExists(projectId);

        return apiTestRepository.findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ApiTestResponse getApiTest(Long projectId, Long testId) {
        return toResponse(findApiTestInProject(projectId, testId));
    }

    public ApiTestResponse updateApiTest(Long projectId, Long testId, ApiTestRequest request) {
        ApiTest apiTest = findApiTestInProject(projectId, testId);
        updateApiTestFields(apiTest, request);

        return toResponse(apiTestRepository.save(apiTest));
    }

    public void deleteApiTest(Long projectId, Long testId) {
        ApiTest apiTest = findApiTestInProject(projectId, testId);
        apiTestRepository.delete(apiTest);
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
        apiTest.setExpectedResponseBody(request.expectedResponseBody());
        apiTest.setExpectedJsonPath(request.expectedJsonPath());
        apiTest.setExpectedJsonValue(request.expectedJsonValue());
        apiTest.setExpectedHeaderName(request.expectedHeaderName());
        apiTest.setExpectedHeaderValue(request.expectedHeaderValue());
        apiTest.setMaxResponseTimeMs(request.maxResponseTimeMs());
        apiTest.setExpectedStatus(request.expectedStatus());
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
