package com.prasad.agentic_software_engineer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.util.Objects;

@ConfigurationProperties(prefix = "agentic.workspace")
public record AgentWorkspaceProperties(
        Path root
) {

    public AgentWorkspaceProperties {
        root = Objects.requireNonNull(root);
    }
}