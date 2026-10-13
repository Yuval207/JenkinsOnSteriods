package com.systemguard.scan.entity;

import com.systemguard.common.model.BaseEntity;
import com.systemguard.common.model.enums.ScanStatus;
import com.systemguard.common.model.enums.ScannerType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "scan_jobs")
public class ScanJob extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scan_run_id", nullable = false)
    private ScanRun scanRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "scanner", nullable = false)
    private ScannerType scanner;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ScanStatus status = ScanStatus.PENDING;

    @Column(name = "finding_count")
    private int findingCount = 0;

    @Column(name = "raw_result_path")
    private String rawResultPath;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message")
    private String errorMessage;

    // Getters and setters
    public ScanRun getScanRun() { return scanRun; }
    public void setScanRun(ScanRun scanRun) { this.scanRun = scanRun; }
    public ScannerType getScanner() { return scanner; }
    public void setScanner(ScannerType scanner) { this.scanner = scanner; }
    public ScanStatus getStatus() { return status; }
    public void setStatus(ScanStatus status) { this.status = status; }
    public int getFindingCount() { return findingCount; }
    public void setFindingCount(int findingCount) { this.findingCount = findingCount; }
    public String getRawResultPath() { return rawResultPath; }
    public void setRawResultPath(String rawResultPath) { this.rawResultPath = rawResultPath; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
