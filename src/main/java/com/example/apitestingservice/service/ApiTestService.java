package com.example.apitestingservice.service;

import com.example.apitestingservice.entity.ApiTest;
import com.example.apitestingservice.entity.Project;
import com.example.apitestingservice.repository.ApiTestRepository;
import com.example.apitestingservice.repository.ProjectRepository;
import io.restassured.RestAssured;
import io.restassured.response.Response;
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

    public ApiTest createApiTest(Long projectId, ApiTest apiTest) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        apiTest.setProject(project);
        return apiTestRepository.save(apiTest);
    }

    public List<ApiTest> getTestsByProjectId(Long projectId) {
        return apiTestRepository.findByProjectId(projectId);
    }

    public String runTest(Long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));

        Response response = RestAssured
                .given()
                .when()
                .get(project.getBaseUrl());

        return "Status: " + response.getStatusCode();
    }
}