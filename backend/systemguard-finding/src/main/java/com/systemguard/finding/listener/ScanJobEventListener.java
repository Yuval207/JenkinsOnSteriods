package com.systemguard.finding.listener;

import com.systemguard.finding.service.FindingService;
import com.systemguard.scan.event.ScannerJobCompletedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for completed scanner jobs and persists findings into the database.
 */
@Component
public class ScanJobEventListener {

    private static final Logger log = LoggerFactory.getLogger(ScanJobEventListener.class);
    private final FindingService findingService;

    public ScanJobEventListener(FindingService findingService) {
        this.findingService = findingService;
    }

    @EventListener
    public void onScannerJobCompleted(ScannerJobCompletedEvent event) {
        try {
            int newFindings = findingService.processResults(
                    event.projectId(),
                    event.scanRunId(),
                    event.scanJobId(),
                    event.scannerType(),
                    event.result()
            );
            log.info("Persisted {} new findings from {} for scan run {}",
                    newFindings, event.scannerType(), event.scanRunId());
        } catch (Exception e) {
            log.error("Failed to process findings from {} for scan run {}: {}",
                    event.scannerType(), event.scanRunId(), e.getMessage(), e);
        }
    }
}
