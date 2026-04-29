package com.example.apitestingservice.controller;

import com.example.apitestingservice.dto.ApiTestRequest;
import com.example.apitestingservice.dto.ApiTestResponse;
import com.example.apitestingservice.model.TestExecutionResponse;
import com.example.apitestingservice.service.ApiTestService;
import com.example.apitestingservice.service.TestExecutionService;
import jakarta.validation.Valid;
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
    public ApiTestResponse createApiTest(@PathVariable Long projectId, @RequestBody @Valid ApiTestRequest request) {
        return apiTestService.createApiTest(projectId, request);
    }

    @GetMapping
    public List<ApiTestResponse> getTestsByProjectId(@PathVariable Long projectId) {
        return apiTestService.getTestsByProjectId(projectId);
    }

    @PostMapping("/run")
    public List<TestExecutionResponse> runProjectTests(@PathVariable Long projectId) {
        return testExecutionService.runProjectTests(projectId);
    }
}
