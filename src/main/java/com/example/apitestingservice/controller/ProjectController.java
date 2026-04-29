package com.example.apitestingservice.controller;

import com.example.apitestingservice.dto.ProjectRequest;
import com.example.apitestingservice.dto.ProjectResponse;
import com.example.apitestingservice.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ProjectResponse createProject(@RequestBody @Valid ProjectRequest request) {
        return projectService.createProject(request);
    }

    @GetMapping
    public List<ProjectResponse> getAllProjects() {
        return projectService.getAllProjects();
    }

    @GetMapping("/{id}")
    public ProjectResponse getProjectById(@PathVariable Long id) {
        return projectService.getProjectById(id);
    }

    @PutMapping("/{id}")
    public ProjectResponse updateProject(
            @PathVariable Long id,
            @RequestBody @Valid ProjectRequest request
    ) {
        return projectService.updateProject(id, request);
    }
    @DeleteMapping("/{id}")
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }
}
