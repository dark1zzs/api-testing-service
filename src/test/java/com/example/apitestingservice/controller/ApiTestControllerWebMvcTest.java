package com.example.apitestingservice.controller;

import com.example.apitestingservice.dto.ApiTestRequest;
import com.example.apitestingservice.dto.ApiTestResponse;
import com.example.apitestingservice.exception.GlobalExceptionHandler;
import com.example.apitestingservice.exception.NotFoundException;
import com.example.apitestingservice.service.ApiTestService;
import com.example.apitestingservice.service.TestExecutionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApiTestController.class)
@Import(GlobalExceptionHandler.class)
class ApiTestControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ApiTestService apiTestService;

    @MockitoBean
    private TestExecutionService testExecutionService;

    @Test
    void shouldUpdateApiTestAndReturn200() throws Exception {
        Long projectId = 1L;
        Long testId = 2L;

        ApiTestResponse response = new ApiTestResponse(
                testId,
                "Updated test",
                "Updated description",
                "USR-UPDATE-200",
                "PUT",
                "/api/users/1",
                "{\"name\":\"Alice\"}",
                "{\"name\":\"Alice\"}",
                "$.name",
                "Alice",
                "Content-Type",
                "application/json",
                1000L,
                200,
                projectId
        );
        when(apiTestService.updateApiTest(projectId, testId, validRequest())).thenReturn(response);

        mockMvc.perform(
                        put("/projects/{projectId}/tests/{testId}", projectId, testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(validRequest()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testId))
                .andExpect(jsonPath("$.name").value("Updated test"))
                .andExpect(jsonPath("$.method").value("PUT"))
                .andExpect(jsonPath("$.expectedStatus").value(200))
                .andExpect(jsonPath("$.projectId").value(projectId));
    }

    @Test
    void shouldReturn400WhenUpdatePayloadInvalid() throws Exception {
        Long projectId = 1L;
        Long testId = 2L;

        ApiTestRequest invalidRequest = new ApiTestRequest(
                "",
                "desc",
                "KEY-1",
                "PATCH",
                "",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        mockMvc.perform(
                        put("/projects/{projectId}/tests/{testId}", projectId, testId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(invalidRequest))
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Request validation failed"))
                .andExpect(jsonPath("$.validationErrors.name").exists())
                .andExpect(jsonPath("$.validationErrors.method").value("Method must be one of: GET, POST, PUT, DELETE"))
                .andExpect(jsonPath("$.validationErrors.endpoint").exists())
                .andExpect(jsonPath("$.validationErrors.expectedStatus").exists());
    }

    @Test
    void shouldReturn204WhenDeleteApiTest() throws Exception {
        Long projectId = 1L;
        Long testId = 2L;

        mockMvc.perform(delete("/projects/{projectId}/tests/{testId}", projectId, testId))
                .andExpect(status().isNoContent());

        verify(apiTestService, times(1)).deleteApiTest(projectId, testId);
    }

    @Test
    void shouldReturn404WhenDeleteApiTestNotFound() throws Exception {
        Long projectId = 1L;
        Long testId = 404L;
        doThrow(new NotFoundException("Test not found in project"))
                .when(apiTestService)
                .deleteApiTest(projectId, testId);

        mockMvc.perform(delete("/projects/{projectId}/tests/{testId}", projectId, testId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Test not found in project"));
    }

    private ApiTestRequest validRequest() {
        return new ApiTestRequest(
                "Updated test",
                "Updated description",
                "USR-UPDATE-200",
                "PUT",
                "/api/users/1",
                "{\"name\":\"Alice\"}",
                "{\"name\":\"Alice\"}",
                "$.name",
                "Alice",
                "Content-Type",
                "application/json",
                1000L,
                200
        );
    }
}
