package com.systemguard.project.dto;

import com.systemguard.project.entity.Project;
import com.systemguard.project.mapper.ProjectMapper;

/**
 * Response DTO specifically tailored for ZIP uploads, highlighting
 * detected languages and build systems prominently.
 */
public class UploadProjectResponse extends ProjectResponse {

    public UploadProjectResponse() {
        super();
    }

    public static UploadProjectResponse fromProject(Project entity) {
        ProjectResponse base = ProjectMapper.toResponse(entity);
        UploadProjectResponse response = new UploadProjectResponse();
        response.setId(base.getId());
        response.setName(base.getName());
        response.setRepositoryUrl(base.getRepositoryUrl());
        response.setSourceType(base.getSourceType());
        response.setOriginalFileName(base.getOriginalFileName());
        response.setDefaultBranch(base.getDefaultBranch());
        response.setDescription(base.getDescription());
        response.setStatus(base.getStatus());
        response.setLastCommitHash(base.getLastCommitHash());
        response.setDetectedLanguages(base.getDetectedLanguages());
        response.setDetectedBuildTools(base.getDetectedBuildTools());
        response.setTags(base.getTags());
        response.setErrorMessage(base.getErrorMessage());
        response.setCreatedAt(base.getCreatedAt());
        response.setUpdatedAt(base.getUpdatedAt());
        return response;
    }
}

