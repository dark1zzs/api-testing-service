package com.example.apitestingservice.service;

import com.example.apitestingservice.dto.ApiTestRequest;
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

    public ApiTest createApiTest(Long projectId, ApiTestRequest request) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        ApiTest apiTest = new ApiTest();
        apiTest.setName(request.name());
        apiTest.setDescription(request.description());
        apiTest.setTestKey(request.testKey());
        apiTest.setMethod(request.method());
        apiTest.setEndpoint(request.endpoint());
        apiTest.setExpectedStatus(request.expectedStatus());
        apiTest.setProject(project);

        return apiTestRepository.save(apiTest);
    }

    public List<ApiTest> getTestsByProjectId(Long projectId) {
        return apiTestRepository.findByProjectId(projectId);
    }

}
