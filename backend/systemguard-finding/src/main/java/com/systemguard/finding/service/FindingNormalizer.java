package com.systemguard.finding.service;

import com.systemguard.common.model.enums.*;
import com.systemguard.finding.entity.Finding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * Normalizes raw scanner output into the unified Finding model.
 * Each scanner produces different JSON structures — this class maps them all
 * to our canonical representation.
 */
@Component
public class FindingNormalizer {

    private static final Logger log = LoggerFactory.getLogger(FindingNormalizer.class);

    /**
     * Normalize a list of raw Semgrep findings into canonical findings.
     */
    public List<Finding> normalizeSemgrep(List<Map<String, Object>> rawFindings,
                                          UUID projectId, UUID scanRunId, UUID scanJobId) {
        List<Finding> findings = new ArrayList<>();
        for (Map<String, Object> raw : rawFindings) {
            try {
                Finding f = new Finding();
                f.setProjectId(projectId);
                f.setScanRunId(scanRunId);
                f.setScanJobId(scanJobId);
                f.setSource(ScannerType.SEMGREP);
                f.setFindingType(FindingType.SAST);

                String checkId = getStr(raw, "check_id");
                f.setRuleId(checkId);
                f.setTitle(extractTitle(checkId));

                Map<String, Object> extra = getMap(raw, "extra");
                if (extra != null) {
                    f.setDescription(getStr(extra, "message"));
                    f.setSeverity(mapSemgrepSeverity(getStr(extra, "severity")));

                    Map<String, Object> metadata = getMap(extra, "metadata");
                    if (metadata != null) {
                        f.setConfidence(mapConfidence(getStr(metadata, "confidence")));
                        Object cweList = metadata.get("cwe");
                        if (cweList instanceof List && !((List<?>) cweList).isEmpty()) {
                            f.setCweId(cleanCweId(((List<?>) cweList).get(0)));
                        } else if (cweList != null) {
                            f.setCweId(cleanCweId(cweList));
                        }
                    }

                    String lines = getStr(extra, "lines");
                    if (lines != null) {
                        f.setCodeSnippet(lines);
                    }
                }

                f.setFilePath(getStr(raw, "path"));
                f.setLineStart(getInt(raw, "start", "line"));
                f.setLineEnd(getInt(raw, "end", "line"));

                f.setFingerprint(generateFingerprint(f));
                Instant now = Instant.now();
                f.setFirstSeen(now);
                f.setLastSeen(now);
                f.setStatus(FindingStatus.OPEN);
                f.setRemediationStatus(RemediationStatus.NONE);

                // Evidence
                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("ruleId", checkId);
                if (f.getCodeSnippet() != null) evidence.put("codeSnippet", f.getCodeSnippet());
                if (extra != null && extra.containsKey("metadata")) {
                    Map<String, Object> meta = getMap(extra, "metadata");
                    if (meta != null && meta.containsKey("references")) {
                        evidence.put("references", meta.get("references"));
                    }
                }
                f.setEvidence(evidence);

                findings.add(f);
            } catch (Exception e) {
                log.warn("Failed to normalize Semgrep finding: {}", e.getMessage());
            }
        }
        return findings;
    }

    /**
     * Normalize Trivy findings.
     */
    public List<Finding> normalizeTrivy(List<Map<String, Object>> rawFindings,
                                        UUID projectId, UUID scanRunId, UUID scanJobId) {
        List<Finding> findings = new ArrayList<>();
        for (Map<String, Object> raw : rawFindings) {
            try {
                Finding f = new Finding();
                f.setProjectId(projectId);
                f.setScanRunId(scanRunId);
                f.setScanJobId(scanJobId);
                f.setSource(ScannerType.TRIVY);

                String type = getStr(raw, "_type");
                String target = getStr(raw, "_target");

                if ("vulnerability".equals(type)) {
                    f.setFindingType(FindingType.DEPENDENCY);
                    f.setCveId(getStr(raw, "VulnerabilityID"));
                    f.setRuleId(getStr(raw, "VulnerabilityID"));
                    f.setTitle(getStr(raw, "Title"));
                    if (f.getTitle() == null || f.getTitle().isBlank()) {
                        f.setTitle(f.getCveId() + " in " + getStr(raw, "PkgName"));
                    }
                    f.setDescription(getStr(raw, "Description"));
                    f.setSeverity(mapTrivySeverity(getStr(raw, "Severity")));
                    f.setPackageName(getStr(raw, "PkgName"));
                    f.setInstalledVersion(getStr(raw, "InstalledVersion"));
                    f.setFixedVersion(getStr(raw, "FixedVersion"));
                    f.setFilePath(target);

                    Map<String, Object> evidence = new LinkedHashMap<>();
                    evidence.put("cveId", f.getCveId());
                    evidence.put("package", f.getPackageName());
                    evidence.put("installedVersion", f.getInstalledVersion());
                    evidence.put("fixedVersion", f.getFixedVersion());
                    Object refs = raw.get("References");
                    if (refs != null) evidence.put("references", refs);
                    f.setEvidence(evidence);

                } else if ("misconfiguration".equals(type)) {
                    f.setFindingType(FindingType.CONTAINER);
                    f.setRuleId(getStr(raw, "ID"));
                    f.setTitle(getStr(raw, "Title"));
                    f.setDescription(getStr(raw, "Description"));
                    f.setSeverity(mapTrivySeverity(getStr(raw, "Severity")));
                    f.setFilePath(target);

                    Map<String, Object> evidence = new LinkedHashMap<>();
                    evidence.put("resolution", getStr(raw, "Resolution"));
                    evidence.put("primaryUrl", getStr(raw, "PrimaryURL"));
                    f.setEvidence(evidence);
                }

                f.setConfidence(Confidence.HIGH);
                f.setFingerprint(generateFingerprint(f));
                Instant now = Instant.now();
                f.setFirstSeen(now);
                f.setLastSeen(now);
                f.setStatus(FindingStatus.OPEN);
                f.setRemediationStatus(RemediationStatus.NONE);

                findings.add(f);
            } catch (Exception e) {
                log.warn("Failed to normalize Trivy finding: {}", e.getMessage());
            }
        }
        return findings;
    }

    /**
     * Normalize Gitleaks findings.
     */
    public List<Finding> normalizeGitleaks(List<Map<String, Object>> rawFindings,
                                            UUID projectId, UUID scanRunId, UUID scanJobId) {
        List<Finding> findings = new ArrayList<>();
        for (Map<String, Object> raw : rawFindings) {
            try {
                Finding f = new Finding();
                f.setProjectId(projectId);
                f.setScanRunId(scanRunId);
                f.setScanJobId(scanJobId);
                f.setSource(ScannerType.GITLEAKS);
                f.setFindingType(FindingType.SECRET);

                f.setRuleId(getStr(raw, "RuleID"));
                f.setTitle("Secret detected: " + getStr(raw, "Description"));
                f.setDescription("Hardcoded secret found: " + getStr(raw, "Description"));
                f.setSeverity(Severity.HIGH);
                f.setConfidence(Confidence.HIGH);

                f.setFilePath(getStr(raw, "File"));
                Object startLine = raw.get("StartLine");
                if (startLine instanceof Number) {
                    f.setLineStart(((Number) startLine).intValue());
                }
                Object endLine = raw.get("EndLine");
                if (endLine instanceof Number) {
                    f.setLineEnd(((Number) endLine).intValue());
                }

                String match = getStr(raw, "Match");
                if (match != null) {
                    // Redact the actual secret value
                    f.setCodeSnippet(match.length() > 20 ?
                            match.substring(0, 10) + "***REDACTED***" : "***REDACTED***");
                }

                Map<String, Object> evidence = new LinkedHashMap<>();
                evidence.put("ruleId", f.getRuleId());
                evidence.put("entropy", raw.get("Entropy"));
                evidence.put("secretRedacted", true);
                f.setEvidence(evidence);

                f.setFingerprint(generateFingerprint(f));
                Instant now = Instant.now();
                f.setFirstSeen(now);
                f.setLastSeen(now);
                f.setStatus(FindingStatus.OPEN);
                f.setRemediationStatus(RemediationStatus.NONE);

                findings.add(f);
            } catch (Exception e) {
                log.warn("Failed to normalize Gitleaks finding: {}", e.getMessage());
            }
        }
        return findings;
    }

    /**
     * Generate a stable fingerprint for deduplication across scan runs.
     * The fingerprint is based on: source + ruleId + filePath + lineStart + codeContext.
     */
    public String generateFingerprint(Finding f) {
        String input = String.join("|",
                nullSafe(f.getSource()),
                nullSafe(f.getRuleId()),
                nullSafe(f.getFilePath()),
                f.getLineStart() != null ? f.getLineStart().toString() : "",
                nullSafe(f.getPackageName()),
                nullSafe(f.getCveId())
        );
        return sha256(input);
    }

    private String nullSafe(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    private Severity mapSemgrepSeverity(String severity) {
        if (severity == null) return Severity.MEDIUM;
        return switch (severity.toUpperCase()) {
            case "ERROR" -> Severity.HIGH;
            case "WARNING" -> Severity.MEDIUM;
            case "INFO" -> Severity.LOW;
            default -> Severity.MEDIUM;
        };
    }

    private Severity mapTrivySeverity(String severity) {
        if (severity == null) return Severity.MEDIUM;
        return switch (severity.toUpperCase()) {
            case "CRITICAL" -> Severity.CRITICAL;
            case "HIGH" -> Severity.HIGH;
            case "MEDIUM" -> Severity.MEDIUM;
            case "LOW" -> Severity.LOW;
            default -> Severity.INFO;
        };
    }

    private Confidence mapConfidence(String confidence) {
        if (confidence == null) return Confidence.MEDIUM;
        return switch (confidence.toUpperCase()) {
            case "HIGH" -> Confidence.HIGH;
            case "MEDIUM" -> Confidence.MEDIUM;
            case "LOW" -> Confidence.LOW;
            default -> Confidence.MEDIUM;
        };
    }

    private String extractTitle(String checkId) {
        if (checkId == null) return "Unknown finding";
        String[] parts = checkId.split("\\.");
        return parts[parts.length - 1].replace("-", " ").replace("_", " ");
    }

    @SuppressWarnings("unchecked")
    private String getStr(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v != null ? v.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> map, String key) {
        Object v = map.get(key);
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    private Integer getInt(Map<String, Object> map, String... keys) {
        Object current = map;
        for (String key : keys) {
            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(key);
            } else {
                return null;
            }
        }
        if (current instanceof Number) {
            return ((Number) current).intValue();
        }
        return null;
    }

    private String cleanCweId(Object raw) {
        if (raw == null) return null;
        String s = String.valueOf(raw).trim();
        if (s.contains(":")) {
            s = s.substring(0, s.indexOf(":")).trim();
        }
        if (s.length() > 64) {
            s = s.substring(0, 64);
        }
        return s;
    }
}
