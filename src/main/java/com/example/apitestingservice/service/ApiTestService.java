package com.example.apitestingservice.service;

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

    public ApiTest createApiTest(Long projectId, ApiTest apiTest) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        apiTest.setProject(project);
        return apiTestRepository.save(apiTest);
    }

    public List<ApiTest> getTestsByProjectId(Long projectId) {
        return apiTestRepository.findByProjectId(projectId);
    }

}
