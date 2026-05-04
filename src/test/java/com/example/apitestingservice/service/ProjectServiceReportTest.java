package com.example.apitestingservice.service;

import com.example.apitestingservice.dto.ProjectReportResponse;
import com.example.apitestingservice.entity.ApiTest;
import com.example.apitestingservice.entity.Project;
import com.example.apitestingservice.entity.TestRun;
import com.example.apitestingservice.repository.ApiTestRepository;
import com.example.apitestingservice.repository.ProjectRepository;
import com.example.apitestingservice.repository.TestRunRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectServiceReportTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private ApiTestRepository apiTestRepository;

    @Mock
    private TestRunRepository testRunRepository;

    @InjectMocks
    private ProjectService projectService;

    @Test
    void shouldIncludeResponseTimePercentilesAcrossAllProjectRuns() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);
        project.setName("Demo");

        ApiTest test = new ApiTest();
        test.setId(10L);
        test.setName("T1");
        test.setTestKey("K1");
        test.setProject(project);

        TestRun lastRun = new TestRun();
        lastRun.setSuccess(true);
        lastRun.setStatusCode(200);
        lastRun.setResponseTimeMs(150L);
        lastRun.setErrorMessage(null);
        lastRun.setExecutedAt(LocalDateTime.parse("2026-05-04T12:00:00"));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(apiTestRepository.findByProjectIdOrderByRunOrderAscIdAsc(projectId)).thenReturn(List.of(test));
        when(testRunRepository.findByApiTestId(10L)).thenReturn(List.of(lastRun));
        when(testRunRepository.findByApiTest_Project_Id(projectId)).thenReturn(List.of(
                runWithMs(100L),
                runWithMs(200L),
                runWithMs(300L),
                runWithMs(400L),
                runWithMs(1000L)
        ));

        ProjectReportResponse report = projectService.getProjectReport(projectId);

        assertEquals(5, report.responseTimeSampleCount());
        assertEquals(300L, report.responseTimeP50Ms());
        assertEquals(880L, report.responseTimeP95Ms());
        assertNotNull(report.tests());
        assertEquals(1, report.tests().size());
    }

    private static TestRun runWithMs(long ms) {
        TestRun run = new TestRun();
        run.setResponseTimeMs(ms);
        return run;
    }
}
