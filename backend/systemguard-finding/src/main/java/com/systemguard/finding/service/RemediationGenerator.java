package com.systemguard.finding.service;

import com.systemguard.finding.entity.Finding;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates automated remediation advice and code snippets for vulnerabilities.
 */
@Service
public class RemediationGenerator {
    
    private static final Logger log = LoggerFactory.getLogger(RemediationGenerator.class);

    public void generate(Finding finding) {
        log.info("Generating remediation for finding: {}", finding.getTitle());
        
        // Mock generation for Phase 1. In production, this would call an LLM API 
        // with the codeSnippet, ruleId, and language context.
        if (finding.getRuleId() != null && finding.getRuleId().contains("SQL")) {
            finding.setRemediationAdvice("Use parameterized queries (PreparedStatement) or an ORM that automatically handles parameterization. Do not concatenate user input directly into SQL strings.");
        } else if (finding.getRuleId() != null && finding.getRuleId().contains("XSS")) {
            finding.setRemediationAdvice("Sanitize and encode all user-supplied data before rendering it in the UI. Use a trusted HTML sanitization library.");
        } else if (finding.getFindingType().name().equals("DEPENDENCY")) {
            finding.setRemediationAdvice(String.format("Upgrade package %s from %s to version %s.", 
                finding.getPackageName(), finding.getInstalledVersion(), finding.getFixedVersion()));
        } else {
            finding.setRemediationAdvice("Review the vulnerability details and apply the recommended patches outlined by the scanner documentation.");
        }
    }
}
