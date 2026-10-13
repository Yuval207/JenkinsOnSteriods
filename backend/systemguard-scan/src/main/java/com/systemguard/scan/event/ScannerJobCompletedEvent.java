package com.systemguard.scan.event;

import com.systemguard.common.model.enums.ScannerType;
import com.systemguard.scan.service.ScannerResult;

import java.util.UUID;

/**
 * Event published when a scanner job finishes successfully with findings.
 */
public record ScannerJobCompletedEvent(
        UUID projectId,
        UUID scanRunId,
        UUID scanJobId,
        ScannerType scannerType,
        ScannerResult result
) {}
