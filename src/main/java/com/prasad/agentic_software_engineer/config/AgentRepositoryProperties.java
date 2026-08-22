package com.prasad.agentic_software_engineer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.Objects;

@ConfigurationProperties(prefix = "agentic.repository")
public record AgentRepositoryProperties(
        Path allowedRoot,
        int maxFiles,
        long maxFileSizeBytes,
        int maxContextCharacters
) {

    public AgentRepositoryProperties {
        allowedRoot = Objects.requireNonNull(allowedRoot);

        if (maxFiles < 1) {
            throw new IllegalArgumentException(
                    "Maximum repository files must be positive"
            );
        }

        if (maxFileSizeBytes < 1) {
            throw new IllegalArgumentException(
                    "Maximum file size must be positive"
            );
        }

        if (maxContextCharacters < 1) {
            throw new IllegalArgumentException(
                    "Maximum context characters must be positive"
            );
        }
    }
}