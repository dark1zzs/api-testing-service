package com.example.apitestingservice.controller;

import com.example.apitestingservice.entity.ApiTest;
import com.example.apitestingservice.model.TestExecutionResponse;
import com.example.apitestingservice.service.ApiTestService;
import com.example.apitestingservice.service.TestExecutionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/tests")
public class ApiTestController {

    private final ApiTestService apiTestService;
    private final TestExecutionService testExecutionService;

    public ApiTestController(ApiTestService apiTestService, TestExecutionService testExecutionService) {
        this.apiTestService = apiTestService;
        this.testExecutionService = testExecutionService;
    }

    @PostMapping
    public ApiTest createApiTest(@PathVariable Long projectId, @RequestBody ApiTest apiTest) {
        return apiTestService.createApiTest(projectId, apiTest);
    }

    @GetMapping
    public List<ApiTest> getTestsByProjectId(@PathVariable Long projectId) {
        return apiTestService.getTestsByProjectId(projectId);
    }

    @PostMapping("/run")
    public List<TestExecutionResponse> runProjectTests(@PathVariable Long projectId) {
        return testExecutionService.runProjectTests(projectId);
    }
}
