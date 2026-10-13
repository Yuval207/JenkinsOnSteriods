package com.systemguard.finding.dto;

import java.util.UUID;

public class AiFixResponse {
    private UUID findingId;
    private UUID projectId;
    private String filePath;
    private String status;
    private String remediationStatus;
    private String title;
    private String ruleId;
    private String vulnerabilityType;
    private String explanation;
    private String diff;
    private String originalSnippet;
    private String fixedSnippet;
    private boolean applied;
    private String errorMessage;

    public AiFixResponse() {}

    public UUID getFindingId() { return findingId; }
    public void setFindingId(UUID findingId) { this.findingId = findingId; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRemediationStatus() { return remediationStatus; }
    public void setRemediationStatus(String remediationStatus) { this.remediationStatus = remediationStatus; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }

    public String getVulnerabilityType() { return vulnerabilityType; }
    public void setVulnerabilityType(String vulnerabilityType) { this.vulnerabilityType = vulnerabilityType; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getDiff() { return diff; }
    public void setDiff(String diff) { this.diff = diff; }

    public String getOriginalSnippet() { return originalSnippet; }
    public void setOriginalSnippet(String originalSnippet) { this.originalSnippet = originalSnippet; }

    public String getFixedSnippet() { return fixedSnippet; }
    public void setFixedSnippet(String fixedSnippet) { this.fixedSnippet = fixedSnippet; }

    public boolean isApplied() { return applied; }
    public void setApplied(boolean applied) { this.applied = applied; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
