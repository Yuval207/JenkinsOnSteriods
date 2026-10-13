package com.systemguard.finding.controller;

import com.systemguard.common.model.enums.*;
import com.systemguard.finding.dto.AiFixResponse;
import com.systemguard.finding.entity.Finding;
import com.systemguard.finding.service.AiCodeFixService;
import com.systemguard.finding.service.FindingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class FindingController {

    private final FindingService findingService;
    private final AiCodeFixService aiCodeFixService;

    public FindingController(FindingService findingService, AiCodeFixService aiCodeFixService) {
        this.findingService = findingService;
        this.aiCodeFixService = aiCodeFixService;
    }

    @PostMapping("/findings/{findingId}/auto-fix")
    public ResponseEntity<AiFixResponse> autoFixFinding(@PathVariable UUID findingId) {
        AiFixResponse response = aiCodeFixService.applyFix(findingId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/projects/{projectId}/auto-fix-all")
    public ResponseEntity<List<AiFixResponse>> autoFixAll(@PathVariable UUID projectId) {
        List<AiFixResponse> responses = aiCodeFixService.applyFixAll(projectId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/projects/{projectId}/findings")
    public ResponseEntity<Page<Finding>> listFindings(
            @PathVariable UUID projectId,
            @RequestParam(required = false) List<Severity> severity,
            @RequestParam(required = false) List<ScannerType> source,
            @RequestParam(required = false) List<FindingStatus> status,
            @RequestParam(required = false) List<FindingType> findingType,
            @PageableDefault(size = 50, sort = "severity", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(findingService.listFindings(projectId, severity, source, status, findingType, pageable));
    }

    @GetMapping("/findings/{findingId}")
    public ResponseEntity<Finding> getFinding(@PathVariable UUID findingId) {
        return ResponseEntity.ok(findingService.getFinding(findingId));
    }

    @PatchMapping("/findings/{findingId}")
    public ResponseEntity<Finding> updateFinding(
            @PathVariable UUID findingId,
            @RequestBody Map<String, String> update) {
        FindingStatus newStatus = FindingStatus.valueOf(update.get("status"));
        String reason = update.get("reason");
        return ResponseEntity.ok(findingService.updateFindingStatus(findingId, newStatus, reason));
    }

    @GetMapping("/projects/{projectId}/findings/summary")
    public ResponseEntity<Map<String, Object>> getSummary(@PathVariable UUID projectId) {
        return ResponseEntity.ok(findingService.getSummary(projectId));
    }
}
