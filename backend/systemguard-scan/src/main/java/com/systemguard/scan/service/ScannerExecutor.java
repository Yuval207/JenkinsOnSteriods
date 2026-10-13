package com.systemguard.scan.service;

import com.systemguard.common.model.enums.ScannerType;

/**
 * Interface for scanner implementations. Each scanner (Semgrep, Trivy, Gitleaks)
 * implements this to execute its analysis against a local repository clone.
 */
public interface ScannerExecutor {

    /**
     * Which scanner this executor handles.
     */
    ScannerType getScannerType();

    /**
     * Execute the scanner against the given repository path.
     * Implementations must NOT modify the repository.
     * Implementations must handle their own timeouts and resource cleanup.
     *
     * @param repoPath absolute path to the local repository clone
     * @return scanner result with raw output and parsed findings
     */
    ScannerResult execute(String repoPath);
}
