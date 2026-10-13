package com.systemguard.project.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    private String name;

    @NotBlank(message = "Repository URL is required")
    private String repositoryUrl;

    private String defaultBranch = "main";
    private String description;
    private List<String> tags;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    public String getDefaultBranch() { return defaultBranch; }
    public void setDefaultBranch(String defaultBranch) { this.defaultBranch = defaultBranch; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
