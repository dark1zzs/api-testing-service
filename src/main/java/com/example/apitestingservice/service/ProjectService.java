package com.example.apitestingservice.service;

import com.example.apitestingservice.dto.ProjectRequest;
import com.example.apitestingservice.dto.ProjectResponse;
import com.example.apitestingservice.entity.Project;
import com.example.apitestingservice.exception.NotFoundException;
import com.example.apitestingservice.repository.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProjectService {

    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public ProjectResponse createProject(ProjectRequest request) {
        Project project = new Project();
        project.setName(request.name());
        project.setBaseUrl(request.baseUrl());
        project.setDescription(request.description());

        return toResponse(projectRepository.save(project));
    }

    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public ProjectResponse getProjectById(Long id) {
        Project project = findProjectById(id);

        return toResponse(project);
    }

    public void deleteProject(Long id) {
        if (!projectRepository.existsById(id)) {
            throw new NotFoundException("Project not found");
        }

        projectRepository.deleteById(id);
    }

    public ProjectResponse updateProject(Long id, ProjectRequest request) {
        Project project = findProjectById(id);

        project.setName(request.name());
        project.setBaseUrl(request.baseUrl());
        project.setDescription(request.description());

        return toResponse(projectRepository.save(project));
    }

    private Project findProjectById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Project not found"));
    }

    private ProjectResponse toResponse(Project project) {
        return new ProjectResponse(
                project.getId(),
                project.getName(),
                project.getBaseUrl(),
                project.getDescription()
        );
    }
}
