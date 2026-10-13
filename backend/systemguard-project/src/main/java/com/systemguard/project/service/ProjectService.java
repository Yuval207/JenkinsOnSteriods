package com.systemguard.project.service;

import com.systemguard.common.exception.ResourceNotFoundException;
import com.systemguard.common.exception.SystemGuardException;
import com.systemguard.common.model.enums.ProjectStatus;
import com.systemguard.project.dto.CreateProjectRequest;
import com.systemguard.project.dto.ProjectResponse;
import com.systemguard.project.entity.Project;
import com.systemguard.project.mapper.ProjectMapper;
import com.systemguard.project.repository.ProjectRepository;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Service for project and repository management.
 * Handles project creation, Git cloning, and build tool detection.
 */
@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final String repoStoragePath;

    public ProjectService(ProjectRepository projectRepository,
                          @Value("${systemguard.storage.repos-path:/data/repos}") String repoStoragePath) {
        this.projectRepository = projectRepository;
        this.repoStoragePath = repoStoragePath;
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        if (projectRepository.existsByRepositoryUrl(request.getRepositoryUrl())) {
            throw new SystemGuardException("DUPLICATE_PROJECT",
                    "A project with this repository URL already exists: " + request.getRepositoryUrl());
        }

        Project project = new Project();
        project.setName(request.getName());
        project.setRepositoryUrl(request.getRepositoryUrl());
        project.setDefaultBranch(request.getDefaultBranch() != null ? request.getDefaultBranch() : "main");
        project.setDescription(request.getDescription());
        if (request.getTags() != null) {
            project.setTags(request.getTags());
        }
        project.setStatus(ProjectStatus.CREATED);

        project = projectRepository.save(project);
        log.info("Created project: {} ({})", project.getName(), project.getId());

        // Start async clone
        cloneRepositoryAsync(project.getId());

        return ProjectMapper.toResponse(project);
    }

    @Transactional(readOnly = true)
    public Page<ProjectResponse> listProjects(Pageable pageable) {
        return projectRepository.findAll(pageable).map(ProjectMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
        return ProjectMapper.toResponse(project);
    }

    @Transactional(readOnly = true)
    public Project getProjectEntity(UUID projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
    }

    @Transactional
    public void deleteProject(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        // Clean up local clone
        if (project.getLocalPath() != null) {
            try {
                deleteDirectory(Path.of(project.getLocalPath()));
            } catch (IOException e) {
                log.warn("Failed to delete local repo for project {}: {}", projectId, e.getMessage());
            }
        }

        projectRepository.delete(project);
        log.info("Deleted project: {} ({})", project.getName(), projectId);
    }

    @Async
    @Transactional
    public void cloneRepositoryAsync(UUID projectId) {
        syncRepository(projectId);
    }

    @Transactional
    public ProjectResponse syncRepository(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        project.setStatus(ProjectStatus.SYNCING);
        project.setErrorMessage(null);
        projectRepository.save(project);

        try {
            Path candidateLocal = Path.of(project.getRepositoryUrl().replaceFirst("^file://", ""));
            Path repoDir;

            if (Files.isDirectory(candidateLocal)) {
                log.info("Project {} uses existing local directory: {}", project.getName(), candidateLocal);
                repoDir = candidateLocal;
                Path gitDir = repoDir.resolve(".git");
                if (Files.exists(gitDir)) {
                    try (Git git = Git.open(repoDir.toFile())) {
                        Ref head = git.getRepository().findRef("HEAD");
                        if (head != null && head.getObjectId() != null) {
                            project.setLastCommitHash(head.getObjectId().getName());
                        }
                    } catch (Exception ge) {
                        log.warn("Could not read git HEAD for {}: {}", repoDir, ge.getMessage());
                    }
                }
                if (project.getLastCommitHash() == null) {
                    project.setLastCommitHash("local-head");
                }
            } else {
                repoDir = Path.of(repoStoragePath, project.getId().toString());

                if (Files.exists(repoDir) && Files.exists(repoDir.resolve(".git"))) {
                    // Pull if already cloned
                    log.info("Pulling repository for project: {}", project.getName());
                    boolean pulled = false;
                    CredentialsProvider creds = getJGitCredentialsProvider();
                    try (Git git = Git.open(repoDir.toFile())) {
                        PullCommand pullCmd = git.pull();
                        if (creds != null) {
                            pullCmd.setCredentialsProvider(creds);
                        }
                        pullCmd.call();
                        Ref head = git.getRepository().findRef("HEAD");
                        if (head != null && head.getObjectId() != null) {
                            project.setLastCommitHash(head.getObjectId().getName());
                        }
                        pulled = true;
                    } catch (Exception ge) {
                        log.warn("JGit pull failed for {}: {}. Attempting native git pull fallback...", project.getName(), ge.getMessage());
                    }

                    if (!pulled) {
                        try {
                            nativeGitPull(repoDir, project.getDefaultBranch());
                            String head = getNativeGitHead(repoDir);
                            if (head != null) {
                                project.setLastCommitHash(head);
                            }
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Native git pull interrupted", ie);
                        }
                    }
                } else {
                    // Clone new
                    log.info("Cloning repository for project: {} from {}", project.getName(), project.getRepositoryUrl());
                    boolean cloned = false;
                    CredentialsProvider creds = getJGitCredentialsProvider();

                    try {
                        Files.createDirectories(repoDir);
                        CloneCommand cloneCmd = Git.cloneRepository()
                                .setURI(project.getRepositoryUrl())
                                .setDirectory(repoDir.toFile())
                                .setBranch(project.getDefaultBranch())
                                .setCloneAllBranches(false);
                        if (creds != null) {
                            cloneCmd.setCredentialsProvider(creds);
                        }
                        try (Git git = cloneCmd.call()) {
                            Ref head = git.getRepository().findRef("HEAD");
                            if (head != null && head.getObjectId() != null) {
                                project.setLastCommitHash(head.getObjectId().getName());
                            }
                        }
                        cloned = true;
                    } catch (Exception ge) {
                        log.warn("JGit clone failed for {}: {}. Attempting native git clone fallback...", project.getName(), ge.getMessage());
                    }

                    if (!cloned) {
                        try {
                            nativeGitClone(project.getRepositoryUrl(), project.getDefaultBranch(), repoDir);
                            String head = getNativeGitHead(repoDir);
                            if (head != null) {
                                project.setLastCommitHash(head);
                            }
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new IOException("Native git clone interrupted", ie);
                        }
                    }
                }
            }

            project.setLocalPath(repoDir.toString());
            detectProjectMetadata(project, repoDir);
            project.setStatus(ProjectStatus.ACTIVE);
            project = projectRepository.save(project);
            log.info("Repository synced for project: {} at {}", project.getName(), repoDir);

        } catch (Exception e) {
            log.error("Failed to sync repository for project: {}", project.getName(), e);
            project.setStatus(ProjectStatus.ERROR);
            project.setErrorMessage("Repository sync failed: " + e.getMessage());
            project = projectRepository.save(project);
        }

        return ProjectMapper.toResponse(project);
    }

    /**
     * Detects languages and build tools from the repository file structure.
     */
    private void detectProjectMetadata(Project project, Path repoDir) {
        Set<String> languages = new LinkedHashSet<>();
        Set<String> buildTools = new LinkedHashSet<>();

        if (Files.exists(repoDir.resolve("pom.xml"))) {
            buildTools.add("Maven");
            languages.add("Java");
        }
        if (Files.exists(repoDir.resolve("build.gradle")) || Files.exists(repoDir.resolve("build.gradle.kts"))) {
            buildTools.add("Gradle");
            languages.add("Java");
        }
        if (Files.exists(repoDir.resolve("package.json"))) {
            buildTools.add("npm");
            languages.add("JavaScript");
        }
        if (Files.exists(repoDir.resolve("Dockerfile"))) {
            buildTools.add("Docker");
        }
        if (Files.exists(repoDir.resolve("docker-compose.yml")) || Files.exists(repoDir.resolve("docker-compose.yaml"))) {
            buildTools.add("Docker Compose");
        }
        if (Files.exists(repoDir.resolve("requirements.txt")) || Files.exists(repoDir.resolve("setup.py"))) {
            languages.add("Python");
            buildTools.add("pip");
        }
        if (Files.exists(repoDir.resolve("go.mod"))) {
            languages.add("Go");
        }
        if (Files.exists(repoDir.resolve("Cargo.toml"))) {
            languages.add("Rust");
            buildTools.add("Cargo");
        }

        // Check for Terraform/IaC
        try {
            if (Files.walk(repoDir, 3).anyMatch(p -> p.toString().endsWith(".tf"))) {
                buildTools.add("Terraform");
            }
        } catch (IOException ignored) {}

        project.setDetectedLanguages(String.join(",", languages));
        project.setDetectedBuildTools(String.join(",", buildTools));
    }

    private CredentialsProvider getJGitCredentialsProvider() {
        String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isBlank()) {
            token = System.getenv("GH_TOKEN");
        }
        if (token == null || token.isBlank()) {
            token = System.getProperty("systemguard.github.token");
        }
        if (token != null && !token.isBlank()) {
            return new UsernamePasswordCredentialsProvider("x-access-token", token.trim());
        }
        return null;
    }

    private void nativeGitClone(String url, String branch, Path repoDir) throws IOException, InterruptedException {
        if (Files.exists(repoDir)) {
            deleteDirectory(repoDir);
        }
        if (repoDir.getParent() != null) {
            Files.createDirectories(repoDir.getParent());
        }

        List<String> cmd = new ArrayList<>(List.of("git", "clone"));
        if (branch != null && !branch.isBlank()) {
            cmd.addAll(List.of("--branch", branch, "--single-branch"));
        }
        cmd.addAll(List.of(url, repoDir.toString()));

        log.info("Executing native git clone: {}", String.join(" ", cmd));
        Process process = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.warn("Native git clone with branch failed (exit {}): {}. Retrying default clone...", exitCode, output);
            if (Files.exists(repoDir)) {
                deleteDirectory(repoDir);
            }
            Process retry = new ProcessBuilder("git", "clone", url, repoDir.toString())
                    .redirectErrorStream(true)
                    .start();
            String retryOutput = new String(retry.getInputStream().readAllBytes());
            int retryExit = retry.waitFor();
            if (retryExit != 0) {
                throw new IOException("Native git clone failed (exit " + retryExit + "): " + retryOutput.trim());
            }
        }
    }

    private void nativeGitPull(Path repoDir, String branch) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of("git", "-C", repoDir.toString(), "pull"));
        if (branch != null && !branch.isBlank()) {
            cmd.addAll(List.of("origin", branch));
        }
        log.info("Executing native git pull: {}", String.join(" ", cmd));
        Process process = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("Native git pull failed (exit " + exitCode + "): " + output.trim());
        }
    }

    private String getNativeGitHead(Path repoDir) {
        try {
            Process process = new ProcessBuilder("git", "-C", repoDir.toString(), "rev-parse", "HEAD")
                    .redirectErrorStream(true)
                    .start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            if (process.waitFor() == 0 && output.length() == 40) {
                return output;
            }
        } catch (Exception e) {
            log.warn("Could not read HEAD via native git for {}: {}", repoDir, e.getMessage());
        }
        return null;
    }

    private void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
