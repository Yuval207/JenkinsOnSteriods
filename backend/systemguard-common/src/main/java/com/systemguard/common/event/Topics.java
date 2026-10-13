package com.systemguard.common.event;

/**
 * Constants for Kafka topic names used across the platform.
 */
public final class Topics {

    private Topics() {}

    public static final String SCAN_REQUESTED = "scan.requested";
    public static final String SCANNER_REQUESTED = "scanner.requested";
    public static final String SCANNER_COMPLETED = "scanner.completed";
    public static final String FINDING_NORMALIZED = "finding.normalized";
    public static final String ANALYSIS_REQUESTED = "analysis.requested";
    public static final String REMEDIATION_REQUESTED = "remediation.requested";
    public static final String REMEDIATION_COMPLETED = "remediation.completed";
    public static final String VERIFICATION_REQUESTED = "verification.requested";
    public static final String VERIFICATION_COMPLETED = "verification.completed";
    public static final String NOTIFICATION_REQUESTED = "notification.requested";
}
