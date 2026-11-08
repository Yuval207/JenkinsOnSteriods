package com.systemguard.project.controller;

import com.systemguard.project.dto.CreateProjectRequest;
import com.systemguard.project.dto.ProjectResponse;
import com.systemguard.project.service.ProjectService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        ProjectResponse response = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProjectResponse> uploadProject(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description) {
        ProjectResponse response = projectService.createProjectFromZip(name, description, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping(value = "/{projectId}/download", produces = "application/zip")
    public ResponseEntity<StreamingResponseBody> downloadProject(@PathVariable UUID projectId) {
        var project = projectService.getProjectEntity(projectId);
        String safeName = (project.getName() != null && !project.getName().isBlank())
                ? project.getName().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "project";
        String filename = safeName + ".zip";

        StreamingResponseBody stream = projectService.downloadProjectAsZip(projectId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(stream);
    }

    @GetMapping
    public ResponseEntity<Page<ProjectResponse>> listProjects(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(projectService.listProjects(pageable));
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(projectService.getProject(projectId));
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable UUID projectId) {
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/sync")
    public ResponseEntity<Map<String, Object>> syncRepository(@PathVariable UUID projectId) {
        projectService.syncRepository(projectId);
        return ResponseEntity.accepted().body(Map.of(
                "projectId", projectId,
                "status", "SYNCING",
                "message", "Repository sync initiated"
        ));
    }
}
