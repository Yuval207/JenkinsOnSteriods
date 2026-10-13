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
 * Trivy vulnerability scanner executor.
 * Scans for dependency vulnerabilities and container misconfigurations.
 */
@Component
public class TrivyExecutor implements ScannerExecutor {

    private static final Logger log = LoggerFactory.getLogger(TrivyExecutor.class);
    private static final int TIMEOUT_SECONDS = 300;
    private final ObjectMapper objectMapper;

    public TrivyExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public ScannerType getScannerType() {
        return ScannerType.TRIVY;
    }

    @Override
    public ScannerResult execute(String repoPath) {
        long startTime = System.currentTimeMillis();
        try {
            Path outputFile = Files.createTempFile("trivy-output-", ".json");

            List<String> command = new ArrayList<>(List.of(
                    "docker", "run", "--rm",
                    "-v", repoPath + ":/src"
            ));
            String userHome = System.getProperty("user.home");
            if (userHome != null && Files.exists(Path.of(userHome, ".m2"))) {
                command.add("-v");
                command.add(userHome + "/.m2:/root/.m2:ro");
            }
            command.addAll(List.of(
                    "--memory=512m",
                    "--cpus=1.0",
                    "aquasec/trivy:latest", "fs",
                    "--format", "json",
                    "--quiet",
                    "--scanners", "vuln,misconfig,secret",
                    "/src"
            ));

            ProcessBuilder pb = new ProcessBuilder(command);

            Process process = pb.start();
            String processOutput;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                processOutput = reader.lines().collect(Collectors.joining("\n"));
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            long durationMs = System.currentTimeMillis() - startTime;

            if (!finished) {
                process.destroyForcibly();
                return ScannerResult.failure(ScannerType.TRIVY, "Trivy timed out after " + TIMEOUT_SECONDS + "s", durationMs);
            }

            String rawOutput = processOutput;
            if (rawOutput != null && rawOutput.contains("{")) {
                rawOutput = rawOutput.substring(rawOutput.indexOf("{"));
            }

            List<Map<String, Object>> findings = parseTrivyOutput(rawOutput);
            log.info("Trivy completed: {} findings in {}ms", findings.size(), durationMs);

            return ScannerResult.success(ScannerType.TRIVY, rawOutput, findings, durationMs);

        } catch (Exception e) {
            long durationMs = System.currentTimeMillis() - startTime;
            log.error("Trivy execution failed", e);
            return ScannerResult.failure(ScannerType.TRIVY, "Trivy execution failed: " + e.getMessage(), durationMs);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseTrivyOutput(String rawJson) {
        try {
            Map<String, Object> output = objectMapper.readValue(rawJson, new TypeReference<>() {});
            List<Map<String, Object>> allFindings = new ArrayList<>();

            Object results = output.get("Results");
            if (results instanceof List) {
                for (Object resultObj : (List<?>) results) {
                    if (resultObj instanceof Map) {
                        Map<String, Object> result = (Map<String, Object>) resultObj;
                        String target = (String) result.getOrDefault("Target", "unknown");

                        // Extract vulnerabilities
                        Object vulns = result.get("Vulnerabilities");
                        if (vulns instanceof List) {
                            for (Object v : (List<?>) vulns) {
                                if (v instanceof Map) {
                                    Map<String, Object> vuln = new HashMap<>((Map<String, Object>) v);
                                    vuln.put("_target", target);
                                    vuln.put("_type", "vulnerability");
                                    allFindings.add(vuln);
                                }
                            }
                        }

                        // Extract misconfigurations
                        Object misconfigs = result.get("Misconfigurations");
                        if (misconfigs instanceof List) {
                            for (Object m : (List<?>) misconfigs) {
                                if (m instanceof Map) {
                                    Map<String, Object> misconfig = new HashMap<>((Map<String, Object>) m);
                                    misconfig.put("_target", target);
                                    misconfig.put("_type", "misconfiguration");
                                    allFindings.add(misconfig);
                                }
                            }
                        }
                    }
                }
            }

            return allFindings;
        } catch (Exception e) {
            log.warn("Failed to parse Trivy output: {}", e.getMessage());
            return List.of();
        }
    }
}
