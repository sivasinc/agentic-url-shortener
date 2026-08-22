package com.prasad.agentic_software_engineer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

@ConfigurationProperties(prefix = "agentic.model")
public record AgentModelProperties(
        String provider,
        URI baseUrl,
        String apiKey,
        String model,
        Duration timeout,
        int maxOutputTokens
) {

    public AgentModelProperties {
        provider = requireText(provider, "Model provider");
        baseUrl = Objects.requireNonNull(baseUrl);
        apiKey = apiKey == null ? "" : apiKey.trim();
        model = requireText(model, "Model name");
        timeout = Objects.requireNonNull(timeout);

        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException(
                    "Model timeout must be positive"
            );
        }

        if (maxOutputTokens < 1) {
            throw new IllegalArgumentException(
                    "Maximum output tokens must be positive"
            );
        }
    }

    private static String requireText(
            String value,
            String field
    ) {
        Objects.requireNonNull(value);

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " cannot be blank"
            );
        }

        return value.trim();
    }
}