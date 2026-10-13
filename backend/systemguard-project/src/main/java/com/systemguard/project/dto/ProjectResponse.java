package com.systemguard.project.dto;

import com.systemguard.common.model.enums.ProjectStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class ProjectResponse {

    private UUID id;
    private String name;
    private String repositoryUrl;
    private String defaultBranch;
    private String description;
    private ProjectStatus status;
    private String lastCommitHash;
    private String detectedLanguages;
    private String detectedBuildTools;
    private List<String> tags;
    private String errorMessage;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ProjectStatus getStatus() { return status; }
    public void setStatus(ProjectStatus status) { this.status = status; }
    public String getLastCommitHash() { return lastCommitHash; }
    public void setLastCommitHash(String lastCommitHash) { this.lastCommitHash = lastCommitHash; }
    public String getDetectedLanguages() { return detectedLanguages; }
    public void setDetectedLanguages(String detectedLanguages) { this.detectedLanguages = detectedLanguages; }
    public String getDetectedBuildTools() { return detectedBuildTools; }
    public void setDetectedBuildTools(String detectedBuildTools) { this.detectedBuildTools = detectedBuildTools; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
