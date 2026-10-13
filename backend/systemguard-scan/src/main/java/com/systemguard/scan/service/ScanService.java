package com.systemguard.scan.service;

import com.systemguard.common.exception.ResourceNotFoundException;
import com.systemguard.common.model.enums.ScanStatus;
import com.systemguard.common.model.enums.ScannerType;
import com.systemguard.common.storage.ObjectStorageService;
import com.systemguard.scan.event.ScannerJobCompletedEvent;
import org.springframework.context.ApplicationEventPublisher;
import com.systemguard.scan.dto.ScanRunResponse;
import com.systemguard.scan.dto.TriggerScanRequest;
import com.systemguard.scan.entity.ScanJob;
import com.systemguard.scan.entity.ScanRun;
import com.systemguard.scan.repository.ScanJobRepository;
import com.systemguard.scan.repository.ScanRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core scan orchestration service.
 * Creates scan runs, dispatches scanner jobs, collects results,
 * persists findings, and publishes SSE events throughout the lifecycle.
 */
@Service
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);
    private static final String ARTIFACTS_BUCKET = "scan-artifacts";

    private final ScanRunRepository scanRunRepository;
    private final ScanJobRepository scanJobRepository;
    private final ScanSseService sseService;
    private final ObjectStorageService objectStorage;
    private final Map<ScannerType, ScannerExecutor> scannerExecutors;
    private final ArchitectureExtractor architectureExtractor;
    private final ApplicationEventPublisher eventPublisher;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private ScanService self;

    public ScanService(ScanRunRepository scanRunRepository,
                       ScanJobRepository scanJobRepository,
                       ScanSseService sseService,
                       ObjectStorageService objectStorage,
                       List<ScannerExecutor> executors,
                       ArchitectureExtractor architectureExtractor,
                       ApplicationEventPublisher eventPublisher) {
        this.scanRunRepository = scanRunRepository;
        this.scanJobRepository = scanJobRepository;
        this.sseService = sseService;
        this.objectStorage = objectStorage;
        this.architectureExtractor = architectureExtractor;
        this.eventPublisher = eventPublisher;
        this.scannerExecutors = executors.stream()
                .collect(Collectors.toMap(ScannerExecutor::getScannerType, e -> e));
    }

    /**
     * Creates a scan run and begins asynchronous scanner execution.
     */
    public ScanRunResponse triggerScan(UUID projectId, String repoLocalPath, String branch,
                                      String commitHash, TriggerScanRequest request) {
        List<ScannerType> scanners = request.getScanners();
        if (scanners == null || scanners.isEmpty()) {
            scanners = List.of(ScannerType.SEMGREP, ScannerType.TRIVY, ScannerType.GITLEAKS);
        }

        UUID correlationId = UUID.randomUUID();

        ScanRun scanRun = new ScanRun();
        scanRun.setProjectId(projectId);
        scanRun.setBranch(branch != null ? branch : "main");
        scanRun.setCommitHash(commitHash);
        scanRun.setStatus(ScanStatus.PENDING);
        scanRun.setCorrelationId(correlationId);

        for (ScannerType scannerType : scanners) {
            ScanJob job = new ScanJob();
            job.setScanner(scannerType);
            job.setStatus(ScanStatus.PENDING);
            scanRun.addJob(job);
        }

        scanRun = scanRunRepository.save(scanRun);
        log.info("Created scan run {} for project {} with scanners: {}", scanRun.getId(), projectId, scanners);

        // Dispatch async execution through self to ensure Spring proxy intercepts it
        self.executeScanAsync(scanRun.getId(), repoLocalPath);

        return toResponse(scanRun);
    }

    /**
     * Asynchronously executes all scanner jobs for a scan run.
     */
    @Async("scanExecutor")
    public void executeScanAsync(UUID scanRunId, String repoLocalPath) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        ScanRun scanRun = null;
        for (int i = 0; i < 10; i++) {
            Optional<ScanRun> opt = scanRunRepository.findByIdWithJobs(scanRunId);
            if (opt.isPresent()) {
                scanRun = opt.get();
                break;
            }
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        if (scanRun == null) {
            throw new ResourceNotFoundException("ScanRun", scanRunId);
        }

        scanRun.setStatus(ScanStatus.RUNNING);
        scanRun.setStartedAt(Instant.now());
        scanRunRepository.save(scanRun);

        sseService.sendEvent(scanRunId, "scan.progress", Map.of(
                "scanId", scanRunId,
                "progress", 0,
                "status", "RUNNING",
                "message", "Scan started"
        ));

        // Extract Architecture Graph before running scanners
        architectureExtractor.extractAndPersist(scanRun.getProjectId(), repoLocalPath);

        List<ScanJob> jobs = scanRun.getJobs();
        int completedCount = 0;
        boolean anyFailed = false;

        for (ScanJob job : jobs) {
            ScannerExecutor executor = scannerExecutors.get(job.getScanner());
            if (executor == null) {
                log.warn("No executor found for scanner: {}", job.getScanner());
                job.setStatus(ScanStatus.FAILED);
                job.setErrorMessage("No executor available for: " + job.getScanner());
                scanJobRepository.save(job);
                anyFailed = true;
                completedCount++;
                continue;
            }

            ScannerResult result = null;
            try {
                job.setStatus(ScanStatus.RUNNING);
                job.setStartedAt(Instant.now());
                scanJobRepository.save(job);

                log.info("Starting {} scan for run {}", job.getScanner(), scanRunId);
                result = executor.execute(repoLocalPath);

                job.setCompletedAt(Instant.now());
                job.setDurationMs(result.getDurationMs());

                if (result.isSuccess()) {
                    job.setStatus(ScanStatus.COMPLETED);
                    job.setFindingCount(result.getRawFindings().size());

                    // Store raw output in object storage
                    if (result.getRawOutput() != null && !result.getRawOutput().isEmpty()) {
                        String artifactKey = String.format("scans/%s/%s-output.json",
                                scanRunId, job.getScanner().name().toLowerCase());
                        objectStorage.putObject(ARTIFACTS_BUCKET, artifactKey,
                                result.getRawOutput().getBytes(StandardCharsets.UTF_8), "application/json");
                        job.setRawResultPath(artifactKey);
                    }

                    // Publish completed scanner job event for finding ingestion
                    try {
                        eventPublisher.publishEvent(new ScannerJobCompletedEvent(
                                scanRun.getProjectId(), scanRunId, job.getId(),
                                job.getScanner(), result));
                    } catch (Exception fe) {
                        log.error("Failed to publish ScannerJobCompletedEvent from {} for scan {}: {}",
                                job.getScanner(), scanRunId, fe.getMessage(), fe);
                    }
                } else {
                    job.setStatus(ScanStatus.FAILED);
                    job.setErrorMessage(result.getErrorMessage());
                    anyFailed = true;
                }

            } catch (Exception e) {
                log.error("Scanner {} failed unexpectedly for scan {}", job.getScanner(), scanRunId, e);
                job.setStatus(ScanStatus.FAILED);
                job.setCompletedAt(Instant.now());
                job.setErrorMessage("Unexpected error: " + e.getMessage());
                anyFailed = true;
            }

            scanJobRepository.save(job);
            completedCount++;

            int progress = (completedCount * 100) / jobs.size();
            scanRun.setProgress(progress);
            scanRunRepository.save(scanRun);

            sseService.sendEvent(scanRunId, "job.completed", Map.of(
                    "scanId", scanRunId,
                    "jobId", job.getId(),
                    "scanner", job.getScanner(),
                    "status", job.getStatus(),
                    "findingCount", job.getFindingCount(),
                    "progress", progress
            ));
        }

        // Finalize scan run
        boolean anySuccess = jobs.stream().anyMatch(j -> j.getStatus() == ScanStatus.COMPLETED);
        scanRun.setStatus(anySuccess ? ScanStatus.COMPLETED : ScanStatus.FAILED);
        scanRun.setProgress(100);
        scanRun.setCompletedAt(Instant.now());

        // Calculate total findings
        int totalFindings = jobs.stream().mapToInt(ScanJob::getFindingCount).sum();
        scanRun.setTotalFindings(totalFindings);

        scanRunRepository.save(scanRun);

        String finalEvent = scanRun.getStatus() == ScanStatus.COMPLETED ? "scan.completed" : "scan.failed";
        sseService.sendEvent(scanRunId, finalEvent, Map.of(
                "scanId", scanRunId,
                "status", scanRun.getStatus(),
                "totalFindings", totalFindings,
                "progress", 100
        ));
        sseService.complete(scanRunId);

        log.info("Scan run {} completed with status {} ({} findings)",
                scanRunId, scanRun.getStatus(), totalFindings);
    }

    @Transactional(readOnly = true)
    public Page<ScanRunResponse> listScans(UUID projectId, Pageable pageable) {
        return scanRunRepository.findByProjectIdOrderByCreatedAtDesc(projectId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ScanRunResponse getScan(UUID scanId) {
        ScanRun scanRun = scanRunRepository.findByIdWithJobs(scanId)
                .orElseThrow(() -> new ResourceNotFoundException("ScanRun", scanId));
        return toResponse(scanRun);
    }

    private ScanRunResponse toResponse(ScanRun scanRun) {
        ScanRunResponse resp = new ScanRunResponse();
        resp.setId(scanRun.getId());
        resp.setProjectId(scanRun.getProjectId());
        resp.setBranch(scanRun.getBranch());
        resp.setCommitHash(scanRun.getCommitHash());
        resp.setStatus(scanRun.getStatus());
        resp.setProgress(scanRun.getProgress());
        resp.setCreatedAt(scanRun.getCreatedAt());
        resp.setCompletedAt(scanRun.getCompletedAt());

        List<ScanRunResponse.ScanJobResponse> jobResponses = scanRun.getJobs().stream().map(job -> {
            ScanRunResponse.ScanJobResponse jr = new ScanRunResponse.ScanJobResponse();
            jr.setId(job.getId());
            jr.setScanner(job.getScanner());
            jr.setStatus(job.getStatus());
            jr.setFindingCount(job.getFindingCount());
            jr.setStartedAt(job.getStartedAt());
            jr.setCompletedAt(job.getCompletedAt());
            jr.setErrorMessage(job.getErrorMessage());
            return jr;
        }).collect(Collectors.toList());
        resp.setJobs(jobResponses);

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalFindings", scanRun.getTotalFindings());
        summary.put("critical", scanRun.getCriticalCount());
        summary.put("high", scanRun.getHighCount());
        summary.put("medium", scanRun.getMediumCount());
        summary.put("low", scanRun.getLowCount());
        resp.setSummary(summary);

        return resp;
    }
}
