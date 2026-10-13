package com.systemguard.scan.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages SSE connections for real-time scan progress updates.
 * Clients subscribe by scanId, and the service broadcasts events as scanner jobs complete.
 */
@Service
public class ScanSseService {

    private static final Logger log = LoggerFactory.getLogger(ScanSseService.class);
    private static final long SSE_TIMEOUT = 600_000L; // 10 minutes

    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Create a new SSE emitter for the given scan.
     */
    public SseEmitter subscribe(UUID scanId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        emitters.computeIfAbsent(scanId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeEmitter(scanId, emitter));
        emitter.onTimeout(() -> removeEmitter(scanId, emitter));
        emitter.onError(e -> removeEmitter(scanId, emitter));

        log.debug("SSE subscriber added for scan: {}", scanId);
        return emitter;
    }

    /**
     * Send an event to all subscribers of a scan.
     */
    public void sendEvent(UUID scanId, String eventName, Object data) {
        CopyOnWriteArrayList<SseEmitter> scanEmitters = emitters.get(scanId);
        if (scanEmitters == null || scanEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : scanEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data));
            } catch (IOException e) {
                log.debug("Failed to send SSE event, removing emitter for scan: {}", scanId);
                removeEmitter(scanId, emitter);
            }
        }
    }

    /**
     * Complete all emitters for a scan (called when scan finishes).
     */
    public void complete(UUID scanId) {
        CopyOnWriteArrayList<SseEmitter> scanEmitters = emitters.remove(scanId);
        if (scanEmitters != null) {
            for (SseEmitter emitter : scanEmitters) {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.debug("Error completing SSE emitter for scan: {}", scanId);
                }
            }
        }
    }

    private void removeEmitter(UUID scanId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> scanEmitters = emitters.get(scanId);
        if (scanEmitters != null) {
            scanEmitters.remove(emitter);
            if (scanEmitters.isEmpty()) {
                emitters.remove(scanId);
            }
        }
    }
}
