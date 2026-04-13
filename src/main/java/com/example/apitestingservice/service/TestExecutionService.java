package com.example.apitestingservice.service;

import com.example.apitestingservice.entity.ApiTest;
import com.example.apitestingservice.entity.TestRun;
import com.example.apitestingservice.model.ExecutionResult;
import com.example.apitestingservice.repository.ApiTestRepository;
import com.example.apitestingservice.repository.TestRunRepository;
import com.example.apitestingservice.tests.ApiTestExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TestExecutionService {

    private final ApiTestRepository apiTestRepository;
    private final TestRunRepository testRunRepository;
    private final ApiTestExecutor apiTestExecutor;

    public TestExecutionService(ApiTestRepository apiTestRepository,
                                TestRunRepository testRunRepository,
                                ApiTestExecutor apiTestExecutor) {
        this.apiTestRepository = apiTestRepository;
        this.testRunRepository = testRunRepository;
        this.apiTestExecutor = apiTestExecutor;
    }

    public ExecutionResult runTest(Long testId) {
        ApiTest test = apiTestRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found"));

        String baseUrl = test.getProject().getBaseUrl();

        ExecutionResult result = apiTestExecutor.execute(
                baseUrl,
                test.getEndpoint(),
                test.getExpectedStatus()
        );

        // 🔥 сохраняем результат
        TestRun testRun = new TestRun();
        testRun.setApiTest(test);
        testRun.setSuccess(result.isSuccess());
        testRun.setStatusCode(result.getStatusCode());
        testRun.setErrorMessage(result.getErrorMessage());
        testRun.setExecutedAt(LocalDateTime.now());

        testRunRepository.save(testRun);

        return result;
    }
}