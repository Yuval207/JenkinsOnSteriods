package com.systemguard.finding.repository;

import com.systemguard.common.model.enums.*;
import com.systemguard.finding.entity.Finding;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FindingRepository extends JpaRepository<Finding, UUID> {

    Page<Finding> findByProjectId(UUID projectId, Pageable pageable);

    List<Finding> findByProjectId(UUID projectId);

    @Query("SELECT f FROM Finding f WHERE f.projectId = :projectId " +
           "AND (:severity IS NULL OR f.severity IN :severity) " +
           "AND (:source IS NULL OR f.source IN :source) " +
           "AND (:status IS NULL OR f.status IN :status) " +
           "AND (:findingType IS NULL OR f.findingType IN :findingType)")
    Page<Finding> findByFilters(@Param("projectId") UUID projectId,
                                @Param("severity") List<Severity> severity,
                                @Param("source") List<ScannerType> source,
                                @Param("status") List<FindingStatus> status,
                                @Param("findingType") List<FindingType> findingType,
                                Pageable pageable);

    List<Finding> findByScanRunId(UUID scanRunId);

    Optional<Finding> findByProjectIdAndFingerprint(UUID projectId, String fingerprint);

    @Query("SELECT f.severity, COUNT(f) FROM Finding f WHERE f.projectId = :projectId AND f.status = 'OPEN' GROUP BY f.severity")
    List<Object[]> countBySeverityForProject(@Param("projectId") UUID projectId);

    @Query("SELECT f.source, COUNT(f) FROM Finding f WHERE f.projectId = :projectId AND f.status = 'OPEN' GROUP BY f.source")
    List<Object[]> countBySourceForProject(@Param("projectId") UUID projectId);

    long countByProjectIdAndStatus(UUID projectId, FindingStatus status);
}
