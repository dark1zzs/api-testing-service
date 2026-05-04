package com.example.apitestingservice.service;

import com.example.apitestingservice.dto.TestRunResponse;
import com.example.apitestingservice.entity.ApiTest;
import com.example.apitestingservice.entity.TestRun;
import com.example.apitestingservice.exception.NotFoundException;
import com.example.apitestingservice.model.ExecutionResult;
import com.example.apitestingservice.model.TestExecutionResponse;
import com.example.apitestingservice.repository.ApiTestRepository;
import com.example.apitestingservice.repository.ProjectRepository;
import com.example.apitestingservice.repository.TestRunRepository;
import com.example.apitestingservice.tests.ApiTestExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TestExecutionService {

    private final ApiTestRepository apiTestRepository;
    private final ProjectRepository projectRepository;
    private final TestRunRepository testRunRepository;
    private final ApiTestExecutor apiTestExecutor;

    public TestExecutionService(ApiTestRepository apiTestRepository,
                                ProjectRepository projectRepository,
                                TestRunRepository testRunRepository,
                                ApiTestExecutor apiTestExecutor) {
        this.apiTestRepository = apiTestRepository;
        this.projectRepository = projectRepository;
        this.testRunRepository = testRunRepository;
        this.apiTestExecutor = apiTestExecutor;
    }

    public ExecutionResult runTest(Long testId) {
        ApiTest test = apiTestRepository.findById(testId)
                .orElseThrow(() -> new NotFoundException("Test not found"));

        TestRun testRun = executeAndSave(test);

        return new ExecutionResult(
                testRun.isSuccess(),
                testRun.getStatusCode(),
                testRun.getResponseBody(),
                testRun.getErrorMessage()
        );
    }

    public List<TestExecutionResponse> runProjectTests(Long projectId) {
        projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));

        return apiTestRepository.findByProjectId(projectId)
                .stream()
                .map(this::executeAndSave)
                .map(this::toResponse)
                .toList();
    }

    public List<TestRunResponse> getTestHistory(Long testId) {
        apiTestRepository.findById(testId)
                .orElseThrow(() -> new NotFoundException("Test not found"));

        return testRunRepository.findByApiTestId(testId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    private TestRun executeAndSave(ApiTest test) {
        ExecutionResult result = apiTestExecutor.execute(
                test.getProject().getBaseUrl(),
                test.getEndpoint(),
                test.getMethod(),
                test.getRequestBody(),
                test.getExpectedResponseBody(),
                test.getExpectedJsonPath(),
                test.getExpectedJsonValue(),
                test.getExpectedHeaderName(),
                test.getExpectedHeaderValue(),
                test.getExpectedStatus()
        );

        LocalDateTime executedAt = LocalDateTime.now();
        TestRun testRun = new TestRun();
        testRun.setApiTest(test);
        testRun.setSuccess(result.isSuccess());
        testRun.setStatusCode(result.getStatusCode());
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
                testRun.getResponseBody(),
                testRun.getErrorMessage(),
                testRun.getExecutedAt()
        );
    }
}
