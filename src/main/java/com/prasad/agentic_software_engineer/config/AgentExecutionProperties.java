package com.prasad.agentic_software_engineer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "agentic.execution")
public record AgentExecutionProperties(
        int maxAttempts,
        Duration commandTimeout,
        int maxOutputCharacters,
        int maxPatchFiles,
        long maxPatchBytes
) {

    public AgentExecutionProperties {
        commandTimeout = Objects.requireNonNull(
                commandTimeout
        );

        if (maxAttempts < 1 ||
                maxOutputCharacters < 1 ||
                maxPatchFiles < 1 ||
                maxPatchBytes < 1) {
            throw new IllegalArgumentException(
                    "Agent execution limits must be positive"
            );
        }

        if (commandTimeout.isZero() ||
                commandTimeout.isNegative()) {
            throw new IllegalArgumentException(
                    "Command timeout must be positive"
            );
        }
    }
}