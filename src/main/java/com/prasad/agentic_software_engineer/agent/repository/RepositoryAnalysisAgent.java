package com.prasad.agentic_software_engineer.agent.repository;

import com.prasad.agentic_software_engineer.tool.repository.RepositoryFile;
import com.prasad.agentic_software_engineer.tool.repository.RepositoryTools;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RepositoryAnalysisAgent {

    private static final Set<String> IGNORED_WORDS =
            Set.of(
                    "with",
                    "from",
                    "that",
                    "this",
                    "into",
                    "existing",
                    "feature",
                    "system",
                    "service",
                    "implement"
            );

    private final RepositoryTools repositoryTools;

    public RepositoryAssessment analyze(
            Path repositoryRoot,
            String requirement
    ) {
        if (requirement == null ||
                requirement.isBlank()) {
            throw new IllegalArgumentException(
                    "Requirement cannot be blank"
            );
        }

        List<RepositoryFile> files =
                repositoryTools.listFiles(
                        repositoryRoot
                );

        List<String> paths = files.stream()
                .map(RepositoryFile::relativePath)
                .toList();

        Set<String> buildSystems =
                detectBuildSystems(paths);

        List<String> modules =
                detectModules(paths);

        List<String> sourceFiles =
                filter(paths, "/src/main/");

        List<String> testFiles =
                filter(paths, "/src/test/");

        List<String> migrations =
                paths.stream()
                        .filter(
                                path ->
                                        path.contains(
                                                "/db/migration/"
                                        ) ||
                                                path.startsWith(
                                                        "src/main/resources/db/migration/"
                                                )
                        )
                        .toList();

        List<String> configurationFiles =
                paths.stream()
                        .filter(this::isConfiguration)
                        .toList();

        List<String> documentationFiles =
                paths.stream()
                        .filter(this::isDocumentation)
                        .toList();

        List<String> impactedFiles =
                identifyImpactedFiles(
                        repositoryRoot,
                        files,
                        requirement
                );

        return new RepositoryAssessment(
                files.size(),
                buildSystems,
                modules,
                sourceFiles,
                testFiles,
                migrations,
                configurationFiles,
                documentationFiles,
                impactedFiles
        );
    }

    private Set<String> detectBuildSystems(
            List<String> paths
    ) {
        Set<String> systems =
                new LinkedHashSet<>();

        for (String path : paths) {
            String filename = filename(path);

            if (filename.equals("pom.xml") ||
                    filename.equals("mvnw") ||
                    filename.equals("mvnw.cmd")) {
                systems.add("MAVEN");
            }

            if (filename.equals("build.gradle") ||
                    filename.equals("build.gradle.kts") ||
                    filename.equals("gradlew")) {
                systems.add("GRADLE");
            }

            if (filename.equals("package.json")) {
                systems.add("NODE");
            }
        }

        return Set.copyOf(systems);
    }

    private List<String> detectModules(
            List<String> paths
    ) {
        return paths.stream()
                .filter(
                        path ->
                                path.endsWith("/pom.xml")
                )
                .map(
                        path ->
                                path.substring(
                                        0,
                                        path.length() -
                                                "/pom.xml".length()
                                )
                )
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> filter(
            List<String> paths,
            String marker
    ) {
        return paths.stream()
                .filter(
                        path ->
                                path.contains(marker) ||
                                        path.startsWith(
                                                marker.substring(1)
                                        )
                )
                .toList();
    }

    private List<String> identifyImpactedFiles(
            Path repositoryRoot,
            List<RepositoryFile> files,
            String requirement
    ) {
        Set<String> tokens =
                requirementTokens(requirement);

        return files.stream()
                .filter(
                        file ->
                                isRelevant(
                                        repositoryRoot,
                                        file,
                                        tokens
                                )
                )
                .map(RepositoryFile::relativePath)
                .limit(50)
                .toList();
    }

    private boolean isRelevant(
            Path repositoryRoot,
            RepositoryFile file,
            Set<String> tokens
    ) {
        String normalizedPath =
                file.relativePath()
                        .toLowerCase(Locale.ROOT);

        if (tokens.stream()
                .anyMatch(normalizedPath::contains)) {
            return true;
        }

        if (!isTextCandidate(file.relativePath())) {
            return false;
        }

        try {
            String content = repositoryTools
                    .readFile(
                            repositoryRoot,
                            file.relativePath()
                    )
                    .toLowerCase(Locale.ROOT);

            return tokens.stream()
                    .anyMatch(content::contains);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private Set<String> requirementTokens(
            String requirement
    ) {
        Set<String> tokens = new HashSet<>();

        Arrays.stream(
                        requirement
                                .toLowerCase(Locale.ROOT)
                                .split("[^a-z0-9]+")
                )
                .map(String::trim)
                .filter(token -> token.length() >= 4)
                .filter(
                        token ->
                                !IGNORED_WORDS.contains(token)
                )
                .forEach(tokens::add);

        return Set.copyOf(tokens);
    }

    private boolean isConfiguration(String path) {
        String lower = path.toLowerCase(Locale.ROOT);

        return lower.endsWith(".yaml") ||
                lower.endsWith(".yml") ||
                lower.endsWith(".properties") ||
                lower.endsWith(".json") ||
                lower.endsWith(".xml") ||
                lower.endsWith(".toml");
    }

    private boolean isDocumentation(String path) {
        String lower = path.toLowerCase(Locale.ROOT);

        return lower.endsWith(".md") ||
                lower.endsWith(".adoc") ||
                lower.endsWith(".txt");
    }

    private boolean isTextCandidate(String path) {
        String lower = path.toLowerCase(Locale.ROOT);

        return lower.endsWith(".java") ||
                lower.endsWith(".kt") ||
                lower.endsWith(".sql") ||
                lower.endsWith(".xml") ||
                lower.endsWith(".yaml") ||
                lower.endsWith(".yml") ||
                lower.endsWith(".properties") ||
                lower.endsWith(".json") ||
                lower.endsWith(".md") ||
                lower.endsWith(".gradle") ||
                lower.endsWith(".kts");
    }

    private String filename(String path) {
        int separator = path.lastIndexOf('/');

        if (separator < 0) {
            return path;
        }

        return path.substring(separator + 1);
    }
}