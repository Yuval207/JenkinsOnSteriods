package com.systemguard.project.entity;

import com.systemguard.common.model.BaseEntity;
import com.systemguard.common.model.enums.ProjectStatus;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
public class Project extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "repository_url", nullable = false)
    private String repositoryUrl;

    @Column(name = "default_branch", nullable = false)
    private String defaultBranch = "main";

    @Column(name = "description")
    private String description;

    @Column(name = "local_path")
    private String localPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProjectStatus status = ProjectStatus.CREATED;

    @Column(name = "last_commit_hash")
    private String lastCommitHash;

    @Column(name = "detected_languages", columnDefinition = "TEXT")
    private String detectedLanguages;

    @Column(name = "detected_build_tools", columnDefinition = "TEXT")
    private String detectedBuildTools;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "project_tags", joinColumns = @JoinColumn(name = "project_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();

    @Column(name = "error_message")
    private String errorMessage;

    // Getters and setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }

    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }

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
}
