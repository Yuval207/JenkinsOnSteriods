package com.systemguard.project.repository;

import com.systemguard.project.entity.Project;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {

    Page<Project> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<Project> findByRepositoryUrl(String repositoryUrl);

    boolean existsByRepositoryUrl(String repositoryUrl);
}
