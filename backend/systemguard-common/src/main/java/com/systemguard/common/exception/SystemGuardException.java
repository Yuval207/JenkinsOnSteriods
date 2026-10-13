package com.systemguard.common.exception;

import java.util.UUID;

/**
 * Base exception for domain-specific errors in SystemGuard.
 */
public class SystemGuardException extends RuntimeException {

    private final String errorCode;
    private final UUID correlationId;

    public SystemGuardException(String message) {
        super(message);
        this.errorCode = "SYSTEMGUARD_ERROR";
        this.correlationId = null;
    }

    public SystemGuardException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.correlationId = null;
    }

    public SystemGuardException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.correlationId = null;
    }

    public String getErrorCode() { return errorCode; }
    public UUID getCorrelationId() { return correlationId; }
}
