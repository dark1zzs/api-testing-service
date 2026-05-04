package com.example.apitestingservice.service;

import com.example.apitestingservice.dto.ApiTestRequest;
import com.example.apitestingservice.dto.ApiTestResponse;
import com.example.apitestingservice.entity.ApiTest;
import com.example.apitestingservice.entity.Project;
import com.example.apitestingservice.exception.NotFoundException;
import com.example.apitestingservice.repository.ApiTestRepository;
import com.example.apitestingservice.repository.ProjectRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiTestServiceTest {

    @Mock
    private ApiTestRepository apiTestRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ApiTestService apiTestService;

    @Test
    void shouldUpdateApiTestInProject() {
        Long projectId = 10L;
        Long testId = 15L;

        ApiTest apiTest = existingApiTest(testId, projectId);
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(apiTestRepository.findByIdAndProjectId(testId, projectId)).thenReturn(Optional.of(apiTest));
        when(apiTestRepository.save(apiTest)).thenReturn(apiTest);

        ApiTestRequest request = request("Updated name", "PUT", "/users/1", 200);

        ApiTestResponse response = apiTestService.updateApiTest(projectId, testId, request);

        assertEquals(testId, response.id());
        assertEquals("Updated name", response.name());
        assertEquals("PUT", response.method());
        assertEquals("/users/1", response.endpoint());
        assertEquals(200, response.expectedStatus());
        verify(apiTestRepository, times(1)).save(apiTest);
    }

    @Test
    void shouldDeleteApiTestFromProject() {
        Long projectId = 7L;
        Long testId = 8L;

        ApiTest apiTest = existingApiTest(testId, projectId);
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(apiTestRepository.findByIdAndProjectId(testId, projectId)).thenReturn(Optional.of(apiTest));

        apiTestService.deleteApiTest(projectId, testId);

        verify(apiTestRepository, times(1)).delete(apiTest);
    }

    @Test
    void shouldThrowNotFoundWhenProjectDoesNotExist() {
        Long missingProjectId = 77L;
        Long testId = 1L;
        when(projectRepository.existsById(missingProjectId)).thenReturn(false);

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> apiTestService.updateApiTest(missingProjectId, testId, request("Name", "GET", "/ping", 200))
        );

        assertEquals("Project not found", exception.getMessage());
        verify(apiTestRepository, never()).findByIdAndProjectId(testId, missingProjectId);
    }

    @Test
    void shouldThrowNotFoundWhenTestNotInProject() {
        Long projectId = 5L;
        Long testId = 99L;
        when(projectRepository.existsById(projectId)).thenReturn(true);
        when(apiTestRepository.findByIdAndProjectId(testId, projectId)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> apiTestService.deleteApiTest(projectId, testId)
        );

        assertEquals("Test not found in project", exception.getMessage());
    }

    @Test
    void shouldCreateApiTestBoundToProject() {
        Long projectId = 123L;
        Project project = new Project();
        project.setId(projectId);
        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        ArgumentCaptor<ApiTest> captor = ArgumentCaptor.forClass(ApiTest.class);
        when(apiTestRepository.save(captor.capture())).thenAnswer(invocation -> {
            ApiTest saved = invocation.getArgument(0);
            saved.setId(999L);
            return saved;
        });

        ApiTestResponse response = apiTestService.createApiTest(projectId, request("Create", "POST", "/users", 201));

        ApiTest savedEntity = captor.getValue();
        assertSame(savedEntity.getProject(), project);
        assertEquals(999L, response.id());
        assertEquals(projectId, response.projectId());
    }

    private ApiTest existingApiTest(Long testId, Long projectId) {
        Project project = new Project();
        project.setId(projectId);

        ApiTest apiTest = new ApiTest();
        apiTest.setId(testId);
        apiTest.setProject(project);
        apiTest.setName("Original");
        apiTest.setMethod("GET");
        apiTest.setEndpoint("/users");
        apiTest.setExpectedStatus(200);
        return apiTest;
    }

    private ApiTestRequest request(
            String name,
            String method,
            String endpoint,
            Integer expectedStatus
    ) {
        return new ApiTestRequest(
                name,
                "description",
                "test-key",
                method,
                endpoint,
                "{\"a\":1}",
                "{\"ok\":true}",
                "$.ok",
                "true",
                "Content-Type",
                "application/json",
                1_000L,
                expectedStatus,
                null,
                null,
                null,
                null
        );
    }
}
