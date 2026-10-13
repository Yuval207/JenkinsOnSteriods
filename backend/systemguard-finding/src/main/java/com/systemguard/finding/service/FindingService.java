package com.systemguard.finding.service;

import com.systemguard.common.exception.ResourceNotFoundException;
import com.systemguard.common.model.enums.*;
import com.systemguard.finding.entity.Finding;
import com.systemguard.finding.repository.FindingRepository;
import com.systemguard.scan.service.ScannerResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing findings — normalization, deduplication, querying, and status updates.
 */
@Service
public class FindingService {

    private static final Logger log = LoggerFactory.getLogger(FindingService.class);

    private final FindingRepository findingRepository;
    private final FindingNormalizer normalizer;
    private final RiskAnalyzer riskAnalyzer;
    private final RemediationGenerator remediationGenerator;

    public FindingService(FindingRepository findingRepository, FindingNormalizer normalizer,
                          RiskAnalyzer riskAnalyzer, RemediationGenerator remediationGenerator) {
        this.findingRepository = findingRepository;
        this.normalizer = normalizer;
        this.riskAnalyzer = riskAnalyzer;
        this.remediationGenerator = remediationGenerator;
    }

    /**
     * Process scanner results: normalize and deduplicate findings.
     * Returns the number of new findings created (not duplicates).
     */
    @Transactional
    public int processResults(UUID projectId, UUID scanRunId, UUID scanJobId,
                              ScannerType scannerType, ScannerResult result) {
        if (!result.isSuccess() || result.getRawFindings().isEmpty()) {
            return 0;
        }

        List<Finding> normalized = switch (scannerType) {
            case SEMGREP -> normalizer.normalizeSemgrep(result.getRawFindings(), projectId, scanRunId, scanJobId);
            case TRIVY -> normalizer.normalizeTrivy(result.getRawFindings(), projectId, scanRunId, scanJobId);
            case GITLEAKS -> normalizer.normalizeGitleaks(result.getRawFindings(), projectId, scanRunId, scanJobId);
            default -> {
                log.warn("No normalizer for scanner: {}", scannerType);
                yield List.<Finding>of();
            }
        };

        int newCount = 0;
        for (Finding finding : normalized) {
            // Deduplicate by fingerprint within the same project
            Optional<Finding> existing = findingRepository
                    .findByProjectIdAndFingerprint(finding.getProjectId(), finding.getFingerprint());

            if (existing.isPresent()) {
                // Update last seen and scan run reference
                Finding e = existing.get();
                e.setLastSeen(Instant.now());
                e.setScanRunId(scanRunId);
                e.setScanJobId(scanJobId);
                findingRepository.save(e);
            } else {
                riskAnalyzer.analyze(finding);
                remediationGenerator.generate(finding);
                findingRepository.save(finding);
                newCount++;
            }
        }

        log.info("Processed {} {} findings for project {} (scan run {}): {} new, {} duplicates",
                normalized.size(), scannerType, projectId, scanRunId, newCount, normalized.size() - newCount);
        return newCount;
    }

    @Transactional(readOnly = true)
    public Page<Finding> listFindings(UUID projectId, List<Severity> severity,
                                      List<ScannerType> source, List<FindingStatus> status,
                                      List<FindingType> findingType, Pageable pageable) {
        return findingRepository.findByFilters(projectId,
                severity != null && !severity.isEmpty() ? severity : null,
                source != null && !source.isEmpty() ? source : null,
                status != null && !status.isEmpty() ? status : null,
                findingType != null && !findingType.isEmpty() ? findingType : null,
                pageable);
    }

    @Transactional(readOnly = true)
    public Finding getFinding(UUID findingId) {
        return findingRepository.findById(findingId)
                .orElseThrow(() -> new ResourceNotFoundException("Finding", findingId));
    }

    @Transactional
    public Finding updateFindingStatus(UUID findingId, FindingStatus newStatus, String reason) {
        Finding finding = getFinding(findingId);
        finding.setStatus(newStatus);
        return findingRepository.save(finding);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSummary(UUID projectId) {
        Map<String, Object> summary = new LinkedHashMap<>();

        long total = findingRepository.countByProjectIdAndStatus(projectId, FindingStatus.OPEN);
        summary.put("totalFindings", total);

        Map<String, Long> bySeverity = new LinkedHashMap<>();
        for (Object[] row : findingRepository.countBySeverityForProject(projectId)) {
            bySeverity.put(row[0].toString(), (Long) row[1]);
        }
        summary.put("bySeverity", bySeverity);

        Map<String, Long> bySource = new LinkedHashMap<>();
        for (Object[] row : findingRepository.countBySourceForProject(projectId)) {
            bySource.put(row[0].toString(), (Long) row[1]);
        }
        summary.put("bySource", bySource);

        return summary;
    }
}
