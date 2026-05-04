package com.example.apitestingservice.controller;

import com.example.apitestingservice.dto.ApiTestRequest;
import com.example.apitestingservice.dto.ApiTestResponse;
import com.example.apitestingservice.model.TestExecutionResponse;
import com.example.apitestingservice.service.ApiTestService;
import com.example.apitestingservice.service.TestExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
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

    @GetMapping("/{testId}")
    public ApiTestResponse getApiTest(@PathVariable Long projectId, @PathVariable Long testId) {
        return apiTestService.getApiTest(projectId, testId);
    }

    @PutMapping("/{testId}")
    @Operation(
            summary = "Update API test",
            description = "Fully updates an API test definition inside the selected project"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Test updated successfully"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation error in request payload",
                    content = @Content(schema = @Schema(implementation = com.example.apitestingservice.model.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Project or test not found",
                    content = @Content(schema = @Schema(implementation = com.example.apitestingservice.model.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = com.example.apitestingservice.model.ErrorResponse.class))
            )
    })
    public ApiTestResponse updateApiTest(
            @Parameter(description = "Project id", example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "Test id", example = "10")
            @PathVariable Long testId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "New test configuration",
                    content = @Content(
                            schema = @Schema(implementation = ApiTestRequest.class),
                            examples = @ExampleObject(
                                    name = "Update user profile test",
                                    value = """
                                            {
                                              "name": "Update profile returns 200",
                                              "description": "Checks that profile update works",
                                              "testKey": "USR-UPDATE-200",
                                              "method": "PUT",
                                              "endpoint": "/api/users/1",
                                              "requestBody": "{\\"name\\":\\"Alice\\"}",
                                              "expectedResponseBody": "\\"id\\":1",
                                              "expectedJsonPath": "$.name",
                                              "expectedJsonValue": "Alice",
                                              "expectedHeaderName": "Content-Type",
                                              "expectedHeaderValue": "application/json",
                                              "maxResponseTimeMs": 1000,
                                              "expectedStatus": 200,
                                              "runOrder": 0,
                                              "requestHeadersJson": null,
                                              "captureJsonPath": null,
                                              "captureVariableName": null
                                            }
                                            """
                            )
                    )
            )
            @RequestBody @Valid ApiTestRequest request
    ) {
        return apiTestService.updateApiTest(projectId, testId, request);
    }

    @DeleteMapping("/{testId}")
    @Operation(
            summary = "Delete API test",
            description = "Removes an API test from the selected project"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Test deleted successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Project or test not found",
                    content = @Content(schema = @Schema(implementation = com.example.apitestingservice.model.ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Unexpected server error",
                    content = @Content(schema = @Schema(implementation = com.example.apitestingservice.model.ErrorResponse.class))
            )
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteApiTest(
            @Parameter(description = "Project id", example = "1")
            @PathVariable Long projectId,
            @Parameter(description = "Test id", example = "10")
            @PathVariable Long testId
    ) {
        apiTestService.deleteApiTest(projectId, testId);
    }

    @PostMapping("/run")
    public List<TestExecutionResponse> runProjectTests(@PathVariable Long projectId) {
        return testExecutionService.runProjectTests(projectId);
    }
}
