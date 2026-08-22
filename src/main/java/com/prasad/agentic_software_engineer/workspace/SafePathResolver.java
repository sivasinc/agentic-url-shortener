package com.prasad.agentic_software_engineer.workspace;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

@Component
public class SafePathResolver {

    public Path resolve(
            Path root,
            String relativePath
    ) {
        if (relativePath == null ||
                relativePath.isBlank()) {
            throw new WorkspaceException(
                    "Relative path cannot be blank"
            );
        }

        try {
            return resolve(
                    root,
                    Path.of(relativePath)
            );
        } catch (InvalidPathException exception) {
            throw new WorkspaceException(
                    "Invalid repository path",
                    exception
            );
        }
    }

    public Path resolve(
            Path root,
            Path relativePath
    ) {
        if (relativePath == null) {
            throw new WorkspaceException(
                    "Relative path cannot be null"
            );
        }

        if (relativePath.isAbsolute()) {
            throw new WorkspaceException(
                    "Absolute paths are not allowed: " +
                            relativePath
            );
        }

        Path normalizedRoot =
                root.toAbsolutePath().normalize();

        Path resolved = normalizedRoot
                .resolve(relativePath)
                .normalize();

        if (!resolved.startsWith(normalizedRoot)) {
            throw new WorkspaceException(
                    "Path escapes the approved root: " +
                            relativePath
            );
        }

        verifyExistingPathDoesNotEscape(
                normalizedRoot,
                resolved
        );

        return resolved;
    }

    private void verifyExistingPathDoesNotEscape(
            Path normalizedRoot,
            Path resolved
    ) {
        if (!Files.exists(normalizedRoot) ||
                !Files.exists(resolved)) {
            return;
        }

        try {
            Path realRoot = normalizedRoot.toRealPath();
            Path realResolved = resolved.toRealPath();

            if (!realResolved.startsWith(realRoot)) {
                throw new WorkspaceException(
                        "Resolved path escapes through a symbolic link"
                );
            }
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Unable to verify repository path",
                    exception
            );
        }
    }
}