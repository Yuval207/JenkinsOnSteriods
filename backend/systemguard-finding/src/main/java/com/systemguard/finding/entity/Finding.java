package com.systemguard.finding.entity;

import com.systemguard.common.model.BaseEntity;
import com.systemguard.common.model.enums.*;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Unified Finding entity — the canonical, scanner-agnostic representation of a security finding.
 * Raw scanner output is stored separately in object storage; this table holds normalized data.
 */
@Entity
@Table(name = "findings", indexes = {
        @Index(name = "idx_findings_project", columnList = "project_id"),
        @Index(name = "idx_findings_scan_run", columnList = "scan_run_id"),
        @Index(name = "idx_findings_fingerprint", columnList = "fingerprint"),
        @Index(name = "idx_findings_severity", columnList = "severity"),
        @Index(name = "idx_findings_status", columnList = "status")
})
public class Finding extends BaseEntity {

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "scan_run_id", nullable = false)
    private UUID scanRunId;

    @Column(name = "scan_job_id")
    private UUID scanJobId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private ScannerType source;

    @Enumerated(EnumType.STRING)
    @Column(name = "finding_type", nullable = false)
    private FindingType findingType;

    @Column(name = "rule_id")
    private String ruleId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "confidence")
    private Confidence confidence;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FindingStatus status = FindingStatus.OPEN;

    @Column(name = "fingerprint", nullable = false)
    private String fingerprint;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "line_start")
    private Integer lineStart;

    @Column(name = "line_end")
    private Integer lineEnd;

    @Column(name = "code_snippet", columnDefinition = "TEXT")
    private String codeSnippet;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "evidence", columnDefinition = "jsonb")
    private Map<String, Object> evidence;

    @Column(name = "first_seen", nullable = false)
    private Instant firstSeen;

    @Column(name = "last_seen", nullable = false)
    private Instant lastSeen;

    @Enumerated(EnumType.STRING)
    @Column(name = "remediation_status", nullable = false)
    private RemediationStatus remediationStatus = RemediationStatus.NONE;

    @Column(name = "cve_id")
    private String cveId;

    @Column(name = "cwe_id")
    private String cweId;

    @Column(name = "package_name")
    private String packageName;

    @Column(name = "installed_version")
    private String installedVersion;

    @Column(name = "fixed_version")
    private String fixedVersion;

    @Column(name = "contextual_risk_score")
    private Double contextualRiskScore;

    @Column(name = "remediation_advice", columnDefinition = "TEXT")
    private String remediationAdvice;

    // Getters and setters
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public UUID getScanRunId() { return scanRunId; }
    public void setScanRunId(UUID scanRunId) { this.scanRunId = scanRunId; }
    public UUID getScanJobId() { return scanJobId; }
    public void setScanJobId(UUID scanJobId) { this.scanJobId = scanJobId; }
    public ScannerType getSource() { return source; }
    public void setSource(ScannerType source) { this.source = source; }
    public FindingType getFindingType() { return findingType; }
    public void setFindingType(FindingType findingType) { this.findingType = findingType; }
    public String getRuleId() { return ruleId; }
    public void setRuleId(String ruleId) { this.ruleId = ruleId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Severity getSeverity() { return severity; }
    public void setSeverity(Severity severity) { this.severity = severity; }
    public Confidence getConfidence() { return confidence; }
    public void setConfidence(Confidence confidence) { this.confidence = confidence; }
    public FindingStatus getStatus() { return status; }
    public void setStatus(FindingStatus status) { this.status = status; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Integer getLineStart() { return lineStart; }
    public void setLineStart(Integer lineStart) { this.lineStart = lineStart; }
    public Integer getLineEnd() { return lineEnd; }
    public void setLineEnd(Integer lineEnd) { this.lineEnd = lineEnd; }
    public String getCodeSnippet() { return codeSnippet; }
    public void setCodeSnippet(String codeSnippet) { this.codeSnippet = codeSnippet; }
    public Map<String, Object> getEvidence() { return evidence; }
    public void setEvidence(Map<String, Object> evidence) { this.evidence = evidence; }
    public Instant getFirstSeen() { return firstSeen; }
    public void setFirstSeen(Instant firstSeen) { this.firstSeen = firstSeen; }
    public Instant getLastSeen() { return lastSeen; }
    public void setLastSeen(Instant lastSeen) { this.lastSeen = lastSeen; }
    public RemediationStatus getRemediationStatus() { return remediationStatus; }
    public void setRemediationStatus(RemediationStatus remediationStatus) { this.remediationStatus = remediationStatus; }
    public String getCveId() { return cveId; }
    public void setCveId(String cveId) { this.cveId = cveId; }
    public String getCweId() { return cweId; }
    public void setCweId(String cweId) { this.cweId = cweId; }
    public String getPackageName() { return packageName; }
    public void setPackageName(String packageName) { this.packageName = packageName; }
    public String getInstalledVersion() { return installedVersion; }
    public void setInstalledVersion(String installedVersion) { this.installedVersion = installedVersion; }
    public String getFixedVersion() { return fixedVersion; }
    public void setFixedVersion(String fixedVersion) { this.fixedVersion = fixedVersion; }
    public Double getContextualRiskScore() { return contextualRiskScore; }
    public void setContextualRiskScore(Double contextualRiskScore) { this.contextualRiskScore = contextualRiskScore; }
    public String getRemediationAdvice() { return remediationAdvice; }
    public void setRemediationAdvice(String remediationAdvice) { this.remediationAdvice = remediationAdvice; }
}
