package com.systemguard.scan.dto;

import com.systemguard.common.model.enums.ScanStatus;
import com.systemguard.common.model.enums.ScannerType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScanRunResponse {
    private UUID id;
    private UUID projectId;
    private String branch;
    private String commitHash;
    private ScanStatus status;
    private int progress;
    private List<ScanJobResponse> jobs;
    private Map<String, Object> summary;
    private Instant createdAt;
    private Instant completedAt;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }
    public ScanStatus getStatus() { return status; }
    public void setStatus(ScanStatus status) { this.status = status; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public List<ScanJobResponse> getJobs() { return jobs; }
    public void setJobs(List<ScanJobResponse> jobs) { this.jobs = jobs; }
    public Map<String, Object> getSummary() { return summary; }
    public void setSummary(Map<String, Object> summary) { this.summary = summary; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public static class ScanJobResponse {
        private UUID id;
        private ScannerType scanner;
        private ScanStatus status;
        private int findingCount;
        private Instant startedAt;
        private Instant completedAt;
        private String errorMessage;

        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public ScannerType getScanner() { return scanner; }
        public void setScanner(ScannerType scanner) { this.scanner = scanner; }
        public ScanStatus getStatus() { return status; }
        public void setStatus(ScanStatus status) { this.status = status; }
        public int getFindingCount() { return findingCount; }
        public void setFindingCount(int findingCount) { this.findingCount = findingCount; }
        public Instant getStartedAt() { return startedAt; }
        public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
        public Instant getCompletedAt() { return completedAt; }
        public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    }
}
