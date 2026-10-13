package com.systemguard.scan.event;

import com.systemguard.common.model.enums.ScannerType;
import java.util.List;
import java.util.UUID;

/**
 * Payload for scan.requested Kafka events.
 */
public class ScanRequestedPayload {
    private UUID scanRunId;
    private String repositoryUrl;
    private String branch;
    private String commitHash;
    private String repoLocalPath;
    private List<ScannerType> scanners;

    public UUID getScanRunId() { return scanRunId; }
    public void setScanRunId(UUID scanRunId) { this.scanRunId = scanRunId; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    public String getBranch() { return branch; }
    public void setBranch(String branch) { this.branch = branch; }
    public String getCommitHash() { return commitHash; }
    public void setCommitHash(String commitHash) { this.commitHash = commitHash; }
    public String getRepoLocalPath() { return repoLocalPath; }
    public void setRepoLocalPath(String repoLocalPath) { this.repoLocalPath = repoLocalPath; }
    public List<ScannerType> getScanners() { return scanners; }
    public void setScanners(List<ScannerType> scanners) { this.scanners = scanners; }
}
