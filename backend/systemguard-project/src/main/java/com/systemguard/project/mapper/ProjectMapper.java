package com.systemguard.project.mapper;

import com.systemguard.project.dto.ProjectResponse;
import com.systemguard.project.entity.Project;

/**
 * Maps between Project entity and DTOs.
 * Keeps entity/DTO separation clean.
 */
public final class ProjectMapper {

    private ProjectMapper() {}

    public static ProjectResponse toResponse(Project entity) {
        ProjectResponse dto = new ProjectResponse();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setRepositoryUrl(entity.getRepositoryUrl());
        dto.setDefaultBranch(entity.getDefaultBranch());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus());
        dto.setLastCommitHash(entity.getLastCommitHash());
        dto.setDetectedLanguages(entity.getDetectedLanguages());
        dto.setDetectedBuildTools(entity.getDetectedBuildTools());
        dto.setTags(entity.getTags());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
}
