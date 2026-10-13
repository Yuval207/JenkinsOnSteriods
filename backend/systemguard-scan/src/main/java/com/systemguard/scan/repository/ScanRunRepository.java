package com.systemguard.scan.repository;

import com.systemguard.scan.entity.ScanRun;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScanRunRepository extends JpaRepository<ScanRun, UUID> {

    Page<ScanRun> findByProjectIdOrderByCreatedAtDesc(UUID projectId, Pageable pageable);

    @Query("SELECT s FROM ScanRun s LEFT JOIN FETCH s.jobs WHERE s.id = :id")
    Optional<ScanRun> findByIdWithJobs(UUID id);
}
