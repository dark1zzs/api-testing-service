package com.example.apitestingservice.controller;

import com.example.apitestingservice.entity.ApiTest;
import com.example.apitestingservice.service.ApiTestService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects/{projectId}/tests")
public class ApiTestController {

    private final ApiTestService apiTestService;

    public ApiTestController(ApiTestService apiTestService) {
        this.apiTestService = apiTestService;
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
    public String runTest(@PathVariable Long projectId) {
        return apiTestService.runTest(projectId);
    }
}