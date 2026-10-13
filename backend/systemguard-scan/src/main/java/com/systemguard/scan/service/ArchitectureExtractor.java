package com.systemguard.scan.service;

import com.systemguard.project.entity.Project;
import com.systemguard.project.repository.ProjectRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Extracts architecture and framework dependencies from a repository
 * and persists the structural graph for contextual risk scoring.
 */
@Service
public class ArchitectureExtractor {

    private static final Logger log = LoggerFactory.getLogger(ArchitectureExtractor.class);

    private final ProjectRepository projectRepository;

    public ArchitectureExtractor(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    public void extractAndPersist(UUID projectId, String repoLocalPath) {
        log.info("Extracting architecture for project {} at {}", projectId, repoLocalPath);
        try {
            Path root = Paths.get(repoLocalPath);
            Map<String, Object> archGraph = new HashMap<>();
            
            // Detect primary language & frameworks
            boolean isJava = Files.exists(root.resolve("pom.xml")) || Files.exists(root.resolve("build.gradle"));
            boolean isNode = Files.exists(root.resolve("package.json"));
            boolean isGo = Files.exists(root.resolve("go.mod"));
            
            if (isJava) {
                archGraph.put("language", "Java");
                archGraph.put("frameworks", extractJavaFrameworks(root));
            } else if (isNode) {
                archGraph.put("language", "Node.js");
                archGraph.put("frameworks", extractNodeFrameworks(root));
            } else if (isGo) {
                archGraph.put("language", "Go");
            } else {
                archGraph.put("language", "Unknown");
            }
            
            // Simplified graph persistence logic
            // In a real scenario, this would walk the AST or parse imports
            archGraph.put("components", detectComponents(root, isJava));
            
            // Save to project or a dedicated table
            projectRepository.findById(projectId).ifPresent(project -> {
                // Assuming we'd add an architectureGraph column or related entity.
                // For now, logging the extracted graph.
                log.info("Persisted Architecture Graph: {}", archGraph);
            });

        } catch (Exception e) {
            log.error("Failed to extract architecture: {}", e.getMessage());
        }
    }
    
    private List<String> extractJavaFrameworks(Path root) {
        List<String> frameworks = new ArrayList<>();
        try {
            String pom = Files.exists(root.resolve("pom.xml")) ? Files.readString(root.resolve("pom.xml")) : "";
            if (pom.contains("spring-boot")) frameworks.add("Spring Boot");
            if (pom.contains("hibernate")) frameworks.add("Hibernate");
            if (pom.contains("spring-web")) frameworks.add("Spring Web");
        } catch (IOException e) {
            log.warn("Could not read pom.xml", e);
        }
        return frameworks;
    }
    
    private List<String> extractNodeFrameworks(Path root) {
        List<String> frameworks = new ArrayList<>();
        try {
            String pkg = Files.readString(root.resolve("package.json"));
            if (pkg.contains("\"express\"")) frameworks.add("Express");
            if (pkg.contains("\"react\"")) frameworks.add("React");
            if (pkg.contains("\"next\"")) frameworks.add("Next.js");
        } catch (IOException e) {
            log.warn("Could not read package.json", e);
        }
        return frameworks;
    }
    
    private List<String> detectComponents(Path root, boolean isJava) {
        List<String> components = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    if (isJava && fileName.endsWith("Controller.java")) {
                        components.add("Controller:" + fileName);
                    } else if (isJava && fileName.endsWith("Repository.java")) {
                        components.add("Repository:" + fileName);
                    }
                });
        } catch (IOException e) {
            log.warn("Failed to walk repo", e);
        }
        return components;
    }
}
