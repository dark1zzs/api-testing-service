package com.example.apitestingservice.service;

import com.example.apitestingservice.dto.ProjectRequest;
import com.example.apitestingservice.dto.ProjectReportResponse;
import com.example.apitestingservice.dto.ProjectResponse;
import com.example.apitestingservice.entity.ApiTest;
import com.example.apitestingservice.entity.Project;
import com.example.apitestingservice.entity.TestRun;
import com.example.apitestingservice.exception.NotFoundException;
import com.example.apitestingservice.repository.ApiTestRepository;
import com.example.apitestingservice.repository.ProjectRepository;
import com.example.apitestingservice.repository.TestRunRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ApiTestRepository apiTestRepository;
    private final TestRunRepository testRunRepository;

    public ProjectService(ProjectRepository projectRepository,
                          ApiTestRepository apiTestRepository,
                          TestRunRepository testRunRepository) {
        this.projectRepository = projectRepository;
        this.apiTestRepository = apiTestRepository;
        this.testRunRepository = testRunRepository;
    }

    public ProjectResponse createProject(ProjectRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setBaseUrl(request.baseUrl());
        project.setDescription(request.description());

        return toResponse(projectRepository.save(project));
    }

    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse getProjectById(Long id) {
        Project project = findProjectById(id);

        return toResponse(project);
    }

    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new NotFoundException("Project not found");
        }

        projectRepository.deleteById(id);
    }

    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = findProjectById(id);

        project.setName(request.name());
        project.setBaseUrl(request.baseUrl());
        project.setDescription(request.description());

        return toResponse(projectRepository.save(project));
    }

    public ProjectReportResponse getProjectReport(Long id) {
        Project project = findProjectById(id);
        List<ApiTest> tests = apiTestRepository.findByProjectId(id);
        List<TestRun> lastRuns = tests.stream()
                .map(test -> getLastRun(test.getId()))
                .flatMap(Optional::stream)
                .toList();

        long totalTests = tests.size();
        long passedTests = lastRuns.stream()
                .filter(TestRun::isSuccess)
                .count();
        long failedTests = lastRuns.stream()
                .filter(testRun -> !testRun.isSuccess())
                .count();
        long notRunTests = totalTests - lastRuns.size();
        double successRate = calculateSuccessRate(passedTests, totalTests);
        LocalDateTime lastRunAt = lastRuns.stream()
                .map(TestRun::getExecutedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new ProjectReportResponse(
                project.getId(),
                project.getName(),
                totalTests,
                passedTests,
                failedTests,
                notRunTests,
                successRate,
                lastRunAt
        );
    }

    private Project findProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getBaseUrl(),
                project.getDescription()
        );
    }

    private Optional<TestRun> getLastRun(Long testId) {
        return testRunRepository.findByApiTestId(testId)
                .stream()
                .max(Comparator.comparing(TestRun::getExecutedAt));
    }

    private double calculateSuccessRate(long passedTests, long totalTests) {
        if (totalTests == 0) {
            return 0.0;
        }

        double rawRate = (passedTests * 100.0) / totalTests;
        return Math.round(rawRate * 100.0) / 100.0;
    }
}
