package com.systemguard.project.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemguard.common.enums.SourceType;
import com.systemguard.common.exception.ResourceNotFoundException;
import com.systemguard.common.exception.SystemGuardException;
import com.systemguard.common.model.enums.ProjectStatus;
import com.systemguard.project.dto.CreateProjectRequest;
import com.systemguard.project.dto.ProjectResponse;
import com.systemguard.project.dto.UploadProjectResponse;
import com.systemguard.project.entity.Project;
import com.systemguard.project.mapper.ProjectMapper;
import com.systemguard.project.repository.ProjectRepository;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullCommand;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Service for project and repository management.
 * Handles project creation, Git cloning, ZIP extraction/download, and build tool detection.
 */
@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private static final long MAX_ZIP_SIZE = 500L * 1024 * 1024; // 500MB

    private final ProjectRepository projectRepository;
    private final String repoStoragePath;
    private final ObjectMapper objectMapper;

    public ProjectService(ProjectRepository projectRepository,
                          @Value("${systemguard.storage.repos-path:/data/repos}") String repoStoragePath,
                          ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.repoStoragePath = repoStoragePath;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
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
        project.setSourceType(SourceType.GIT);
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

    /**
     * Creates a project from an uploaded ZIP archive.
     */
    @Transactional
    public ProjectResponse createProjectFromZip(String name, String description, MultipartFile zipFile) {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new SystemGuardException("INVALID_FILE", "ZIP file is required and cannot be empty");
        }

        if (zipFile.getSize() > MAX_ZIP_SIZE) {
            throw new SystemGuardException("FILE_TOO_LARGE", "ZIP file size exceeds the 500MB limit");
        }

        String originalFilename = zipFile.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            originalFilename = "project.zip";
        }

        // Determine fallback name from filename if name is not explicitly provided
        String derivedName = name;
        if (derivedName == null || derivedName.isBlank()) {
            String base = originalFilename.replaceAll("(?i)\\.zip$", "");
            derivedName = base.isBlank() ? "Uploaded Project" : base;
        }

        Project project = new Project();
        project.setName(derivedName.trim());
        project.setDescription(description != null && !description.isBlank() ? description.trim() : null);
        project.setSourceType(SourceType.ZIP_UPLOAD);
        project.setOriginalFileName(originalFilename);
        project.setDefaultBranch("main");
        project.setStatus(ProjectStatus.SYNCING);

        project = projectRepository.save(project);
        log.info("Initialized ZIP project entity: {} ({})", project.getName(), project.getId());

        Path projectDir = Path.of(repoStoragePath, project.getId().toString());

        try {
            Files.createDirectories(projectDir);

            // Extract ZIP entries safely
            try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    Path targetPath = projectDir.resolve(entry.getName()).normalize();
                    // Zip Slip security prevention check
                    if (!targetPath.startsWith(projectDir)) {
                        throw new SystemGuardException("ZIP_SLIP_ATTACK", "Invalid entry in zip: " + entry.getName());
                    }
                    if (entry.isDirectory()) {
                        Files.createDirectories(targetPath);
                    } else {
                        if (targetPath.getParent() != null) {
                            Files.createDirectories(targetPath.getParent());
                        }
                        Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    zis.closeEntry();
                }
            }

            // Handle nested root folder (if ZIP has single root dir, flatten it)
            flattenNestedRootDirectory(projectDir);

            // If name or description was not explicitly provided, attempt auto-detection from package.json or pom.xml
            boolean userProvidedName = name != null && !name.isBlank();
            boolean userProvidedDesc = description != null && !description.isBlank();
            enrichMetadataFromFiles(project, projectDir, userProvidedName, userProvidedDesc);

            // Detect languages and build tools
            detectProjectMetadata(project, projectDir);

            project.setLocalPath(projectDir.toString());
            project.setLastCommitHash("zip-upload");
            project.setStatus(ProjectStatus.ACTIVE);
            project = projectRepository.save(project);

            log.info("Successfully extracted and activated ZIP project: {} ({}) at {}", project.getName(), project.getId(), projectDir);
            return UploadProjectResponse.fromProject(project);

        } catch (Exception e) {
            log.error("Failed to process ZIP upload for project: {}", project.getName(), e);
            project.setStatus(ProjectStatus.ERROR);
            project.setErrorMessage("ZIP processing failed: " + e.getMessage());
            projectRepository.save(project);
            throw new SystemGuardException("ZIP_PROCESSING_FAILED", "Failed to extract and process ZIP file: " + e.getMessage(), e);
        }
    }

    /**
     * Streams the project files as a ZIP archive, excluding build artifacts and VCS data.
     */
    @Transactional(readOnly = true)
    public StreamingResponseBody downloadProjectAsZip(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        if (project.getLocalPath() == null || project.getLocalPath().isBlank()) {
            throw new SystemGuardException("NO_LOCAL_PATH", "Project local path is not configured");
        }

        Path rootPath = Path.of(project.getLocalPath());
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            throw new SystemGuardException("DIRECTORY_NOT_FOUND", "Project directory does not exist on disk: " + rootPath);
        }

        return outputStream -> {
            try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
                Files.walkFileTree(rootPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        String dirName = dir.getFileName() != null ? dir.getFileName().toString() : "";
                        // Exclude .git/, node_modules/, target/, __MACOSX, and build folders
                        if (dirName.equals(".git") || dirName.equals("node_modules") || dirName.equals("target") || dirName.equals("__MACOSX")) {
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        String fileName = file.getFileName().toString();
                        // Exclude .class files and system metadata files
                        if (fileName.endsWith(".class") || fileName.equals(".DS_Store")) {
                            return FileVisitResult.CONTINUE;
                        }
                        Path rel = rootPath.relativize(file);
                        ZipEntry entry = new ZipEntry(rel.toString().replace('\\', '/'));
                        zos.putNextEntry(entry);
                        Files.copy(file, zos);
                        zos.closeEntry();
                        return FileVisitResult.CONTINUE;
                    }
                });
                zos.finish();
            }
        };
    }

    /**
     * Flattens single nested root folder if the ZIP contents were wrapped in one top-level directory.
     */
    private void flattenNestedRootDirectory(Path projectDir) throws IOException {
        List<Path> nonSystemEntries;
        try (var stream = Files.list(projectDir)) {
            nonSystemEntries = stream
                    .filter(p -> {
                        String n = p.getFileName().toString();
                        return !n.equals("__MACOSX") && !n.equals(".DS_Store");
                    })
                    .toList();
        }

        if (nonSystemEntries.size() == 1 && Files.isDirectory(nonSystemEntries.get(0))) {
            Path singleDir = nonSystemEntries.get(0);
            log.info("Detected single nested root folder '{}', flattening to project root", singleDir.getFileName());
            Path tempDir = Files.createTempDirectory("systemguard-flatten-");
            try {
                // Move children of singleDir to tempDir
                try (var subStream = Files.list(singleDir)) {
                    for (Path child : subStream.toList()) {
                        Files.move(child, tempDir.resolve(child.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
                // Delete singleDir
                deleteDirectory(singleDir);

                // Move items from tempDir to projectDir
                try (var tempStream = Files.list(tempDir)) {
                    for (Path child : tempStream.toList()) {
                        Files.move(child, projectDir.resolve(child.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                    }
                }
            } finally {
                deleteDirectory(tempDir);
            }
        }
    }

    /**
     * Reads package.json or pom.xml to enrich project name and description if not explicitly set.
     */
    private void enrichMetadataFromFiles(Project project, Path repoDir, boolean userProvidedName, boolean userProvidedDesc) {
        Path packageJson = repoDir.resolve("package.json");
        if (Files.exists(packageJson)) {
            try {
                JsonNode node = objectMapper.readTree(packageJson.toFile());
                if (!userProvidedName && node.hasNonNull("name")) {
                    String pkgName = node.get("name").asText().trim();
                    if (!pkgName.isEmpty()) {
                        project.setName(pkgName);
                    }
                }
                if (!userProvidedDesc && node.hasNonNull("description")) {
                    String pkgDesc = node.get("description").asText().trim();
                    if (!pkgDesc.isEmpty()) {
                        project.setDescription(pkgDesc);
                    }
                }
            } catch (Exception e) {
                log.warn("Could not read package.json metadata: {}", e.getMessage());
            }
        }

        Path pomXml = repoDir.resolve("pom.xml");
        if (Files.exists(pomXml) && !userProvidedName && (project.getName().equals("Uploaded Project") || project.getName().startsWith("project"))) {
            try {
                String pomContent = Files.readString(pomXml);
                Matcher m = Pattern.compile("<artifactId>([^<]+)</artifactId>").matcher(pomContent);
                if (m.find()) {
                    project.setName(m.group(1).trim());
                }
            } catch (Exception e) {
                log.warn("Could not read pom.xml metadata: {}", e.getMessage());
            }
        }
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

        // Clean up local files
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

        if (project.getSourceType() == SourceType.ZIP_UPLOAD) {
            log.info("Project {} is a ZIP upload; refreshing metadata on local files", project.getName());
            if (project.getLocalPath() != null) {
                Path localDir = Path.of(project.getLocalPath());
                if (Files.isDirectory(localDir)) {
                    detectProjectMetadata(project, localDir);
                    project.setStatus(ProjectStatus.ACTIVE);
                    project.setErrorMessage(null);
                    project = projectRepository.save(project);
                }
            }
            return ProjectMapper.toResponse(project);
        }

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
        if (Files.exists(repoDir.resolve("requirements.txt")) || Files.exists(repoDir.resolve("setup.py")) || Files.exists(repoDir.resolve("pyproject.toml"))) {
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

        // C# / .NET (*.csproj or *.sln)
        try (var stream = Files.walk(repoDir, 3)) {
            if (stream.anyMatch(p -> p.toString().endsWith(".csproj") || p.toString().endsWith(".sln"))) {
                languages.add("C#");
                buildTools.add(".NET");
            }
        } catch (IOException ignored) {}

        // Ruby (Gemfile)
        if (Files.exists(repoDir.resolve("Gemfile"))) {
            languages.add("Ruby");
            buildTools.add("Bundler");
        }

        // PHP (composer.json)
        if (Files.exists(repoDir.resolve("composer.json"))) {
            languages.add("PHP");
            buildTools.add("Composer");
        }

        // Dart / Flutter (pubspec.yaml)
        if (Files.exists(repoDir.resolve("pubspec.yaml"))) {
            languages.add("Dart");
            buildTools.add("Flutter");
        }

        // Check for Terraform/IaC
        try (var stream = Files.walk(repoDir, 3)) {
            if (stream.anyMatch(p -> p.toString().endsWith(".tf"))) {
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
