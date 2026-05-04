package com.example.apitestingservice.service;

import com.example.apitestingservice.entity.ApiTest;
import com.example.apitestingservice.entity.Project;
import com.example.apitestingservice.model.ApiTestExecutionRequest;
import com.example.apitestingservice.model.ExecutionResult;
import com.example.apitestingservice.repository.ApiTestRepository;
import com.example.apitestingservice.repository.ProjectRepository;
import com.example.apitestingservice.repository.TestRunRepository;
import com.example.apitestingservice.tests.ApiTestExecutor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestExecutionServiceBatchTest {

    @Mock
    private ApiTestRepository apiTestRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TestRunRepository testRunRepository;

    @Mock
    private ApiTestExecutor apiTestExecutor;

    private TestExecutionService testExecutionService;

    @BeforeEach
    void setUp() {
        testExecutionService = new TestExecutionService(
                apiTestRepository,
                projectRepository,
                testRunRepository,
                apiTestExecutor,
                new ObjectMapper()
        );
    }

    @Test
    void shouldPassCapturedTokenIntoFollowingRequestHeaders() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        project.setBaseUrl("http://localhost:9");

        ApiTest login = new ApiTest();
        login.setId(10L);
        login.setProject(project);
        login.setMethod("POST");
        login.setEndpoint("/auth/login");
        login.setRequestBody("{}");
        login.setExpectedStatus(200);
        login.setCaptureJsonPath("$.token");
        login.setCaptureVariableName("token");
        login.setRunOrder(0);

        ApiTest booking = new ApiTest();
        booking.setId(11L);
        booking.setProject(project);
        booking.setMethod("POST");
        booking.setEndpoint("/booking");
        booking.setRequestBody("{}");
        booking.setExpectedStatus(201);
        booking.setRequestHeadersJson("{\"Authorization\":\"{{token}}\"}");
        booking.setRunOrder(1);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(apiTestRepository.findByProjectIdOrderByRunOrderAscIdAsc(projectId)).thenReturn(List.of(login, booking));
        when(testRunRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        when(apiTestExecutor.execute(any())).thenAnswer(inv -> {
            ApiTestExecutionRequest req = inv.getArgument(0);
            if (req.endpoint().contains("login")) {
                return new ExecutionResult(true, 200, 1L, "{\"token\":\"secret\"}", null);
            }
            assertEquals("secret", req.requestHeaders().get("Authorization"));
            return new ExecutionResult(true, 201, 1L, "{}", null);
        });

        testExecutionService.runProjectTests(projectId);

        ArgumentCaptor<ApiTestExecutionRequest> captor = ArgumentCaptor.forClass(ApiTestExecutionRequest.class);
        verify(apiTestExecutor, times(2)).execute(captor.capture());
        assertEquals("secret", captor.getAllValues().get(1).requestHeaders().get("Authorization"));
    }
}
