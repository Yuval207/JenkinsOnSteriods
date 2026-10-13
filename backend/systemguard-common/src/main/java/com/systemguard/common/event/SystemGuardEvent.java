package com.systemguard.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.util.UUID;

/**
 * Base Kafka event envelope. Every event published to Kafka wraps its payload in this structure.
 * The eventId serves as the idempotency key for consumers.
 */
public class SystemGuardEvent<T> {

    private UUID eventId;
    private String eventType;

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant timestamp;

    private UUID projectId;
    private UUID correlationId;
    private T payload;

    public SystemGuardEvent() {}

    public SystemGuardEvent(String eventType, UUID projectId, UUID correlationId, T payload) {
        this.eventId = UUID.randomUUID();
        this.eventType = eventType;
        this.timestamp = Instant.now();
        this.projectId = projectId;
        this.correlationId = correlationId;
        this.payload = payload;
    }

    public static <T> SystemGuardEvent<T> of(String eventType, UUID projectId, UUID correlationId, T payload) {
        return new SystemGuardEvent<>(eventType, projectId, correlationId, payload);
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID correlationId) { this.correlationId = correlationId; }
    public T getPayload() { return payload; }
    public void setPayload(T payload) { this.payload = payload; }
}
