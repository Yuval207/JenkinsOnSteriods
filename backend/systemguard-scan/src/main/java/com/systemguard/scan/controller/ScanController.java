package com.systemguard.scan.controller;

import com.systemguard.common.model.enums.ProjectStatus;
import com.systemguard.project.entity.Project;
import com.systemguard.project.service.ProjectService;
import com.systemguard.scan.dto.ScanRunResponse;
import com.systemguard.scan.dto.TriggerScanRequest;
import com.systemguard.scan.service.ScanService;
import com.systemguard.scan.service.ScanSseService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ScanController {

    private final ScanService scanService;
    private final ScanSseService sseService;
    private final ProjectService projectService;

    public ScanController(ScanService scanService, ScanSseService sseService, ProjectService projectService) {
        this.scanService = scanService;
        this.sseService = sseService;
        this.projectService = projectService;
    }

    @PostMapping("/projects/{projectId}/scans")
    public ResponseEntity<ScanRunResponse> triggerScan(@PathVariable UUID projectId,
                                                       @RequestBody TriggerScanRequest request) {
        Project project = projectService.getProjectEntity(projectId);
        if (project.getStatus() != ProjectStatus.ACTIVE) {
            if (project.getLocalPath() != null && java.nio.file.Files.isDirectory(java.nio.file.Path.of(project.getLocalPath()))) {
                // Repository exists locally on disk
            } else {
                try {
                    projectService.syncRepository(projectId);
                    project = projectService.getProjectEntity(projectId);
                } catch (Exception ignored) {
                }
            }
        }

        if (project.getStatus() != ProjectStatus.ACTIVE || project.getLocalPath() == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .build();
        }

        String branch = request.getBranch() != null ? request.getBranch() : project.getDefaultBranch();
        ScanRunResponse response = scanService.triggerScan(projectId, project.getLocalPath(),
                branch, project.getLastCommitHash(), request);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/projects/{projectId}/scans")
    public ResponseEntity<Page<ScanRunResponse>> listScans(
            @PathVariable UUID projectId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(scanService.listScans(projectId, pageable));
    }

    @GetMapping("/scans/{scanId}")
    public ResponseEntity<ScanRunResponse> getScan(@PathVariable UUID scanId) {
        return ResponseEntity.ok(scanService.getScan(scanId));
    }

    @GetMapping(value = "/scans/{scanId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamScanEvents(@PathVariable UUID scanId) {
        return sseService.subscribe(scanId);
    }
}
