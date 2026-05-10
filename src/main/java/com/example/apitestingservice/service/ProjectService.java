package com.example.apitestingservice.service;

import com.example.apitestingservice.dto.ProjectRequest;
import com.example.apitestingservice.dto.ProjectReportResponse;
import com.example.apitestingservice.dto.ProjectReportRunResponse;
import com.example.apitestingservice.dto.ProjectReportTestResponse;
import com.example.apitestingservice.dto.ProjectReportTrendResponse;
import com.example.apitestingservice.dto.ProjectResponse;
import com.example.apitestingservice.entity.ApiTest;
import com.example.apitestingservice.entity.Project;
import com.example.apitestingservice.entity.TestRun;
import com.example.apitestingservice.exception.NotFoundException;
import com.example.apitestingservice.repository.ApiTestRepository;
import com.example.apitestingservice.repository.ProjectRepository;
import com.example.apitestingservice.repository.TestRunRepository;
import com.example.apitestingservice.util.ResponseTimePercentiles;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
        List<ApiTest> tests = apiTestRepository.findByProjectIdOrderByRunOrderAscIdAsc(id);
        List<ProjectReportTestResponse> testReports = tests.stream()
                .map(this::toReportTestResponse)
                .toList();
        List<ProjectReportTestResponse> executedTestReports = testReports.stream()
                .filter(testReport -> testReport.lastRunAt() != null)
                .toList();

        long totalTests = tests.size();
        long passedTests = executedTestReports.stream()
                .filter(ProjectReportTestResponse::success)
                .count();
        long failedTests = executedTestReports.stream()
                .filter(testReport -> !testReport.success())
                .count();
        long notRunTests = totalTests - executedTestReports.size();
        double successRate = calculateSuccessRate(passedTests, totalTests);
        LocalDateTime lastRunAt = executedTestReports.stream()
                .map(ProjectReportTestResponse::lastRunAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        List<TestRun> projectRuns = testRunRepository.findByApiTest_Project_Id(id);
        List<Long> responseTimesMs = projectRuns.stream()
                .map(TestRun::getResponseTimeMs)
                .toList();
        ResponseTimePercentiles.Result latency = ResponseTimePercentiles.fromMillis(responseTimesMs);
        List<ProjectReportRunResponse> recentRuns = buildRecentRuns(projectRuns);
        List<ProjectReportTrendResponse> trend = buildTrend(projectRuns);
        Long averageResponseTimeMs = calculateAverageResponseTime(responseTimesMs);
        Long lastRunTotalDurationMs = recentRuns.isEmpty() ? null : recentRuns.getFirst().totalDurationMs();

        return new ProjectReportResponse(
                project.getId(),
                project.getName(),
                totalTests,
                passedTests,
                failedTests,
                notRunTests,
                successRate,
                lastRunAt,
                latency.sampleCount(),
                latency.p50Ms(),
                latency.p95Ms(),
                projectRuns.size(),
                averageResponseTimeMs,
                lastRunTotalDurationMs,
                testReports,
                recentRuns,
                trend
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

    private ProjectReportTestResponse toReportTestResponse(ApiTest test) {
        return getLastRun(test.getId())
                .map(testRun -> toExecutedReportTestResponse(test, testRun))
                .orElseGet(() -> toNotRunReportTestResponse(test));
    }

    private ProjectReportTestResponse toExecutedReportTestResponse(ApiTest test, TestRun testRun) {
        return new ProjectReportTestResponse(
                test.getId(),
                test.getName(),
                test.getTestKey(),
                testRun.isSuccess(),
                testRun.getStatusCode(),
                testRun.getResponseTimeMs(),
                testRun.getErrorMessage(),
                testRun.getExecutedAt()
        );
    }

    private ProjectReportTestResponse toNotRunReportTestResponse(ApiTest test) {
        return new ProjectReportTestResponse(
                test.getId(),
                test.getName(),
                test.getTestKey(),
                false,
                null,
                null,
                null,
                null
        );
    }

    private double calculateSuccessRate(long passedTests, long totalTests) {
        if (totalTests == 0) {
            return 0.0;
        }

        double rawRate = (passedTests * 100.0) / totalTests;
        return Math.round(rawRate * 100.0) / 100.0;
    }

    private List<ProjectReportRunResponse> buildRecentRuns(List<TestRun> runs) {
        return runs.stream()
                .filter(testRun -> testRun.getExecutedAt() != null)
                .collect(Collectors.groupingBy(
                        testRun -> testRun.getExecutedAt().truncatedTo(ChronoUnit.SECONDS)
                ))
                .entrySet()
                .stream()
                .map(entry -> toRunResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ProjectReportRunResponse::startedAt).reversed())
                .limit(10)
                .toList();
    }

    private ProjectReportRunResponse toRunResponse(LocalDateTime startedAt, List<TestRun> runs) {
        long passedCount = runs.stream()
                .filter(TestRun::isSuccess)
                .count();
        long failedCount = runs.size() - passedCount;
        long totalDurationMs = runs.stream()
                .map(TestRun::getResponseTimeMs)
                .filter(ms -> ms != null && ms >= 0)
                .mapToLong(Long::longValue)
                .sum();

        return new ProjectReportRunResponse(
                startedAt,
                runs.size(),
                passedCount,
                failedCount,
                totalDurationMs
        );
    }

    private List<ProjectReportTrendResponse> buildTrend(List<TestRun> runs) {
        Map<LocalDate, List<TestRun>> runsByDate = runs.stream()
                .filter(testRun -> testRun.getExecutedAt() != null)
                .collect(Collectors.groupingBy(testRun -> testRun.getExecutedAt().toLocalDate()));

        return runsByDate.entrySet()
                .stream()
                .map(entry -> toTrendResponse(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(ProjectReportTrendResponse::date))
                .toList();
    }

    private ProjectReportTrendResponse toTrendResponse(LocalDate date, List<TestRun> runs) {
        long passedCount = runs.stream()
                .filter(TestRun::isSuccess)
                .count();
        long failedCount = runs.size() - passedCount;
        long totalDurationMs = runs.stream()
                .map(TestRun::getResponseTimeMs)
                .filter(ms -> ms != null && ms >= 0)
                .mapToLong(Long::longValue)
                .sum();

        return new ProjectReportTrendResponse(
                date,
                runs.size(),
                passedCount,
                failedCount,
                totalDurationMs
        );
    }

    private Long calculateAverageResponseTime(List<Long> responseTimesMs) {
        List<Long> validResponseTimes = responseTimesMs.stream()
                .filter(ms -> ms != null && ms >= 0)
                .toList();
        if (validResponseTimes.isEmpty()) {
            return null;
        }

        double average = validResponseTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0);
        return Math.round(average);
    }
}
