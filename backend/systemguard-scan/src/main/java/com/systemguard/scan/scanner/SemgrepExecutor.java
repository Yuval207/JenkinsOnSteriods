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
 * Semgrep SAST scanner executor.
 * Runs semgrep with --json output and parses the results.
 */
@Component
public class SemgrepExecutor implements ScannerExecutor {

    private static final Logger log = LoggerFactory.getLogger(SemgrepExecutor.class);
    private static final int TIMEOUT_SECONDS = 300;
    private final ObjectMapper objectMapper;

    public SemgrepExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ScannerType getScannerType() {
        return ScannerType.SEMGREP;
    }

    @Override
    public ScannerResult execute(String repoPath) {
        long startTime = System.currentTimeMillis();
        try {
            Path outputFile = Files.createTempFile("semgrep-output-", ".json");

            ProcessBuilder pb = new ProcessBuilder(
                    "docker", "run", "--rm",
                    "-v", repoPath + ":/src",
                    "--memory=512m",
                    "--cpus=1.0",
                    "returntocorp/semgrep", "semgrep", "scan",
                    "--config", "auto",
                    "--json",
                    "--quiet",
                    "--no-git-ignore",
                    "/src"
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
                return ScannerResult.failure(ScannerType.SEMGREP, "Semgrep timed out after " + TIMEOUT_SECONDS + "s", durationMs);
            }

            // Semgrep returns exit code 1 when findings are present, that's not an error
            // The output is now in stdout since we didn't use --output (Docker volume mapping for output is complex)
            // But wait, our reader already collected stdout!
            String rawOutput = processOutput;
            // Clean up JSON extraction if docker pulled image (skip lines until {)
            if (rawOutput != null && rawOutput.contains("{")) {
                rawOutput = rawOutput.substring(rawOutput.indexOf("{"));
            }

            List<Map<String, Object>> findings = parseSemgrepOutput(rawOutput);
            log.info("Semgrep completed: {} findings in {}ms", findings.size(), durationMs);

            return ScannerResult.success(ScannerType.SEMGREP, rawOutput, findings, durationMs);

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("Semgrep execution failed", e);
            return ScannerResult.failure(ScannerType.SEMGREP, "Semgrep execution failed: " + e.getMessage(), durationMs);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseSemgrepOutput(String rawJson) {
        try {
            Map<String, Object> output = objectMapper.readValue(rawJson, new TypeReference<>() {});
            Object results = output.get("results");
            if (results instanceof List) {
                return (List<Map<String, Object>>) results;
            }
            return List.of();
        } catch (Exception e) {
            log.warn("Failed to parse Semgrep output: {}", e.getMessage());
            return List.of();
        }
    }
}
