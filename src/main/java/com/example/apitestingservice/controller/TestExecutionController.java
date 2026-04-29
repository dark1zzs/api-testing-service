package com.example.apitestingservice.controller;

import com.example.apitestingservice.dto.TestRunResponse;
import com.example.apitestingservice.model.ExecutionResult;
import com.example.apitestingservice.service.TestExecutionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tests")
public class TestExecutionController {

    private final TestExecutionService testExecutionService;

    public TestExecutionController(TestExecutionService testExecutionService) {
        this.testExecutionService = testExecutionService;
    }

    @PostMapping("/{testId}/run")
    public ExecutionResult runTest(@PathVariable Long testId) {
        return testExecutionService.runTest(testId);
    }

    @GetMapping("/{testId}/history")
    public List<TestRunResponse> getHistory(@PathVariable Long testId) {
        return testExecutionService.getTestHistory(testId);
    }
}
