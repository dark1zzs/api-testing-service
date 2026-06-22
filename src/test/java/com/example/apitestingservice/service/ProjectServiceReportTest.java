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
import static org.mockito.Mockito.inOrder;
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
    void shouldDeleteProjectWithTestsAndRunHistory() {
        Long projectId = 1L;
        Project project = new Project();
        project.setId(projectId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));

        projectService.deleteProject(projectId);

        var order = inOrder(testRunRepository, apiTestRepository, projectRepository);
        order.verify(testRunRepository).deleteByApiTest_Project_Id(projectId);
        order.verify(apiTestRepository).deleteByProjectId(projectId);
        order.verify(projectRepository).delete(project);
    }

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

    @Test
    void shouldFilterReportRunsByWeekPeriod() {
        Long projectId = 2L;
        Project project = new Project();
        project.setId(projectId);
        project.setName("Period demo");

        ApiTest test = new ApiTest();
        test.setId(20L);
        test.setName("T1");
        test.setTestKey("K1");
        test.setProject(project);

        TestRun freshRun = runWithMs(120L);
        freshRun.setSuccess(true);
        freshRun.setStatusCode(200);
        freshRun.setExecutedAt(LocalDateTime.now().minusDays(2));

        TestRun oldRun = runWithMs(900L);
        oldRun.setSuccess(false);
        oldRun.setStatusCode(500);
        oldRun.setExecutedAt(LocalDateTime.now().minusDays(20));

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(apiTestRepository.findByProjectIdOrderByRunOrderAscIdAsc(projectId)).thenReturn(List.of(test));
        when(testRunRepository.findByApiTestId(20L)).thenReturn(List.of(oldRun, freshRun));
        when(testRunRepository.findByApiTest_Project_Id(projectId)).thenReturn(List.of(oldRun, freshRun));

        ProjectReportResponse report = projectService.getProjectReport(projectId, "WEEK");

        assertEquals(1, report.totalRuns());
        assertEquals(1, report.responseTimeSampleCount());
        assertEquals(120L, report.averageResponseTimeMs());
        assertEquals(1, report.passedTests());
        assertEquals(0, report.failedTests());
        assertEquals(0, report.notRunTests());
    }

    @Test
    void shouldGroupRecentRunsByExecutionGroupId() {
        Long projectId = 3L;
        Project project = new Project();
        project.setId(projectId);
        project.setName("Grouped demo");

        ApiTest first = new ApiTest();
        first.setId(30L);
        first.setName("T1");
        first.setTestKey("K1");
        first.setProject(project);

        String groupId = "batch-1";
        TestRun firstRun = runWithMs(100L);
        firstRun.setSuccess(true);
        firstRun.setExecutedAt(LocalDateTime.parse("2026-06-22T19:24:01"));
        firstRun.setExecutionGroupId(groupId);

        TestRun secondRun = runWithMs(200L);
        secondRun.setSuccess(false);
        secondRun.setExecutedAt(LocalDateTime.parse("2026-06-22T19:24:03"));
        secondRun.setExecutionGroupId(groupId);

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(apiTestRepository.findByProjectIdOrderByRunOrderAscIdAsc(projectId)).thenReturn(List.of(first));
        when(testRunRepository.findByApiTestId(30L)).thenReturn(List.of(secondRun));
        when(testRunRepository.findByApiTest_Project_Id(projectId)).thenReturn(List.of(firstRun, secondRun));

        ProjectReportResponse report = projectService.getProjectReport(projectId);

        assertEquals(1, report.recentRuns().size());
        assertEquals(2, report.recentRuns().getFirst().testsCount());
        assertEquals(1, report.recentRuns().getFirst().passedCount());
        assertEquals(1, report.recentRuns().getFirst().failedCount());
        assertEquals(300L, report.recentRuns().getFirst().totalDurationMs());
    }

    private static TestRun runWithMs(long ms) {
        TestRun run = new TestRun();
        run.setResponseTimeMs(ms);
        return run;
    }
}
