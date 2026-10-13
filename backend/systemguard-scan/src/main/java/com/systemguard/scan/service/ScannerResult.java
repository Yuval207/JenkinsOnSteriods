package com.systemguard.scan.service;

import com.systemguard.common.model.enums.ScannerType;

import java.util.List;
import java.util.Map;

/**
 * Result of a scanner execution containing raw output and normalized finding data.
 */
public class ScannerResult {
    private final ScannerType scanner;
    private final boolean success;
    private final String rawOutput;
    private final List<Map<String, Object>> rawFindings;
    private final String errorMessage;
    private final long durationMs;

    public ScannerResult(ScannerType scanner, boolean success, String rawOutput,
                         List<Map<String, Object>> rawFindings, String errorMessage, long durationMs) {
        this.scanner = scanner;
        this.success = success;
        this.rawOutput = rawOutput;
        this.rawFindings = rawFindings;
        this.errorMessage = errorMessage;
        this.durationMs = durationMs;
    }

    public static ScannerResult success(ScannerType scanner, String rawOutput,
                                        List<Map<String, Object>> rawFindings, long durationMs) {
        return new ScannerResult(scanner, true, rawOutput, rawFindings, null, durationMs);
    }

    public static ScannerResult failure(ScannerType scanner, String error, long durationMs) {
        return new ScannerResult(scanner, false, null, List.of(), error, durationMs);
    }

    public ScannerType getScanner() { return scanner; }
    public boolean isSuccess() { return success; }
    public String getRawOutput() { return rawOutput; }
    public List<Map<String, Object>> getRawFindings() { return rawFindings; }
    public String getErrorMessage() { return errorMessage; }
    public long getDurationMs() { return durationMs; }
}
