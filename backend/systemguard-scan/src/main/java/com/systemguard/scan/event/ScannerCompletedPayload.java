package com.systemguard.scan.event;

import com.systemguard.common.model.enums.ScannerType;
import java.util.UUID;

/**
 * Payload for scanner.completed Kafka events.
 */
public class ScannerCompletedPayload {
    private UUID scanRunId;
    private UUID scanJobId;
    private ScannerType scanner;
    private String status;
    private String rawResultPath;
    private int findingCount;
    private long durationMs;
    private String error;

    public UUID getScanRunId() { return scanRunId; }
    public void setScanRunId(UUID scanRunId) { this.scanRunId = scanRunId; }
    public UUID getScanJobId() { return scanJobId; }
    public void setScanJobId(UUID scanJobId) { this.scanJobId = scanJobId; }
    public ScannerType getScanner() { return scanner; }
    public void setScanner(ScannerType scanner) { this.scanner = scanner; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRawResultPath() { return rawResultPath; }
    public void setRawResultPath(String rawResultPath) { this.rawResultPath = rawResultPath; }
    public int getFindingCount() { return findingCount; }
    public void setFindingCount(int findingCount) { this.findingCount = findingCount; }
    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
