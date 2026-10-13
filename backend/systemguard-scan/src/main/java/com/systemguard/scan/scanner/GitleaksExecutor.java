package com.systemguard.scan.scanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemguard.common.model.enums.ScannerType;
import com.systemguard.scan.service.ScannerExecutor;
import com.systemguard.scan.service.ScannerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Gitleaks secret scanner executor.
 * Detects hardcoded secrets, API keys, and credentials in source code.
 */
@Component
public class GitleaksExecutor implements ScannerExecutor {

    private static final Logger log = LoggerFactory.getLogger(GitleaksExecutor.class);
    private static final int TIMEOUT_SECONDS = 120;
    private final ObjectMapper objectMapper;

    public GitleaksExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ScannerType getScannerType() {
        return ScannerType.GITLEAKS;
    }

    @Override
    public ScannerResult execute(String repoPath) {
        long startTime = System.currentTimeMillis();
        try {
            Path outputFile = Files.createTempFile("gitleaks-output-", ".json");

            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-v", repoPath + ":/src",
                    "--memory=512m",
                    "--cpus=1.0",
                    "zricethezav/gitleaks:latest", "detect",
                    "--source", "/src",
                    "--report-path", "-",
                    "--report-format", "json",
                    "--no-banner",
                    "--no-color",
                    "--no-git"
            );

            Process process = pb.start();
            String processOutput;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                processOutput = reader.lines().collect(Collectors.joining("\n"));
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long durationMs = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return ScannerResult.failure(ScannerType.GITLEAKS, "Gitleaks timed out after " + TIMEOUT_SECONDS + "s", durationMs);
            }

            String rawOutput = processOutput;
            if (rawOutput != null && rawOutput.contains("[")) {
                rawOutput = rawOutput.substring(rawOutput.indexOf("["));
            }

            // Gitleaks returns exit code 1 when leaks are found
            List<Map<String, Object>> findings = parseGitleaksOutput(rawOutput);
            log.info("Gitleaks completed: {} findings in {}ms", findings.size(), durationMs);

            return ScannerResult.success(ScannerType.GITLEAKS, rawOutput, findings, durationMs);

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("Gitleaks execution failed", e);
            return ScannerResult.failure(ScannerType.GITLEAKS, "Gitleaks execution failed: " + e.getMessage(), durationMs);
        }
    }

    private List<Map<String, Object>> parseGitleaksOutput(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Failed to parse Gitleaks output: {}", e.getMessage());
            return List.of();
        }
    }
}
