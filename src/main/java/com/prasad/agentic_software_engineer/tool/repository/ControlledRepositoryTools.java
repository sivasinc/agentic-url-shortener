package com.prasad.agentic_software_engineer.tool.repository;

import com.prasad.agentic_software_engineer.config.AgentRepositoryProperties;
import com.prasad.agentic_software_engineer.workspace.SafePathResolver;
import com.prasad.agentic_software_engineer.workspace.WorkspaceException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ControlledRepositoryTools
        implements RepositoryTools {

    private final SafePathResolver safePathResolver;
    private final AgentRepositoryProperties properties;

    @Override
    public List<RepositoryFile> listFiles(
            Path repositoryRoot
    ) {
        List<RepositoryFile> files =
                new ArrayList<>();

        try (Stream<Path> paths =
                     Files.walk(repositoryRoot)) {
            paths.sorted()
                    .forEach(
                            path -> inspectPath(
                                    repositoryRoot,
                                    path,
                                    files
                            )
                    );

            return List.copyOf(files);
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Unable to list repository files",
                    exception
            );
        }
    }

    @Override
    public String readFile(
            Path repositoryRoot,
            String relativePath
    ) {
        Path file = safePathResolver.resolve(
                repositoryRoot,
                relativePath
        );

        if (!Files.isRegularFile(file)) {
            throw new WorkspaceException(
                    "Repository file does not exist: " +
                            relativePath
            );
        }

        try {
            long size = Files.size(file);

            if (size > properties.maxFileSizeBytes()) {
                throw new WorkspaceException(
                        "Repository file exceeds maximum size: " +
                                relativePath
                );
            }

            byte[] bytes = Files.readAllBytes(file);

            if (isBinary(bytes)) {
                throw new WorkspaceException(
                        "Binary files cannot be read as model context: " +
                                relativePath
                );
            }

            String content =
                    new String(
                            bytes,
                            StandardCharsets.UTF_8
                    );

            if (content.length() >
                    properties.maxContextCharacters()) {
                throw new WorkspaceException(
                        "Repository file exceeds context limit: " +
                                relativePath
                );
            }

            return content;
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Unable to read repository file: " +
                            relativePath,
                    exception
            );
        }
    }

    @Override
    public List<RepositorySearchMatch> search(
            Path repositoryRoot,
            String query
    ) {
        if (query == null || query.isBlank()) {
            throw new WorkspaceException(
                    "Search query cannot be blank"
            );
        }

        String normalizedQuery =
                query.toLowerCase(Locale.ROOT);

        List<RepositorySearchMatch> matches =
                new ArrayList<>();

        for (RepositoryFile file :
                listFiles(repositoryRoot)) {
            String content;

            try {
                content = readFile(
                        repositoryRoot,
                        file.relativePath()
                );
            } catch (WorkspaceException exception) {
                continue;
            }

            String[] lines = content.split(
                    "\\R",
                    -1
            );

            for (int index = 0;
                 index < lines.length;
                 index++) {
                if (lines[index]
                        .toLowerCase(Locale.ROOT)
                        .contains(normalizedQuery)) {
                    matches.add(
                            new RepositorySearchMatch(
                                    file.relativePath(),
                                    index + 1,
                                    abbreviate(lines[index])
                            )
                    );
                }
            }
        }

        return List.copyOf(matches);
    }

    private void inspectPath(
            Path repositoryRoot,
            Path path,
            List<RepositoryFile> files
    ) {
        if (Files.isSymbolicLink(path)) {
            throw new WorkspaceException(
                    "Symbolic links are not allowed: " +
                            repositoryRoot.relativize(path)
            );
        }

        if (!Files.isRegularFile(path)) {
            return;
        }

        if (files.size() >= properties.maxFiles()) {
            throw new WorkspaceException(
                    "Repository exceeds maximum file count of " +
                            properties.maxFiles()
            );
        }

        try {
            long size = Files.size(path);

            if (size >
                    properties.maxFileSizeBytes()) {
                throw new WorkspaceException(
                        "Repository file exceeds maximum size: " +
                                repositoryRoot.relativize(path)
                );
            }

            files.add(
                    new RepositoryFile(
                            normalizePath(
                                    repositoryRoot.relativize(path)
                            ),
                            size
                    )
            );
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Unable to inspect repository file",
                    exception
            );
        }
    }

    private boolean isBinary(byte[] bytes) {
        int inspected = Math.min(
                bytes.length,
                4096
        );

        for (int index = 0;
             index < inspected;
             index++) {
            if (bytes[index] == 0) {
                return true;
            }
        }

        return false;
    }

    private String abbreviate(String line) {
        String trimmed = line.trim();

        if (trimmed.length() <= 300) {
            return trimmed;
        }

        return trimmed.substring(0, 300);
    }

    private String normalizePath(Path path) {
        return path.toString().replace('\\', '/');
    }
}