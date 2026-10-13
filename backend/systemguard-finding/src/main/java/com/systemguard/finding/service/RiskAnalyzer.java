package com.systemguard.finding.service;

import com.systemguard.finding.entity.Finding;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calculates a contextual risk score for a finding based on the architectural graph.
 */
@Service
public class RiskAnalyzer {
    
    private static final Logger log = LoggerFactory.getLogger(RiskAnalyzer.class);

    public void analyze(Finding finding) {
        log.info("Calculating contextual risk for finding: {}", finding.getTitle());
        
        double baseScore = getBaseScore(finding.getSeverity().name());
        double contextMultiplier = 1.0;
        
        String filePath = finding.getFilePath();
        if (filePath != null) {
            // Internet-facing or authentication components increase risk
            if (filePath.contains("Controller") || filePath.contains("Auth")) {
                contextMultiplier = 1.5;
            } 
            // Internal batch or scripts have lower relative risk
            else if (filePath.contains("batch") || filePath.contains("scripts")) {
                contextMultiplier = 0.8;
            }
        }
        
        double contextualRiskScore = Math.min(10.0, baseScore * contextMultiplier);
        finding.setContextualRiskScore(Math.round(contextualRiskScore * 10.0) / 10.0);
        log.debug("Assigned contextual risk score: {} to finding {}", finding.getContextualRiskScore(), finding.getId());
    }
    
    private double getBaseScore(String severity) {
        return switch (severity) {
            case "CRITICAL" -> 9.0;
            case "HIGH" -> 7.0;
            case "MEDIUM" -> 5.0;
            case "LOW" -> 3.0;
            default -> 1.0;
        };
    }
}
