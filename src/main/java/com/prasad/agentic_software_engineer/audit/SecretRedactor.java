package com.prasad.agentic_software_engineer.audit;

import com.prasad.agentic_software_engineer.config.AgentModelProperties;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class SecretRedactor {

    private static final String REDACTED = "[REDACTED]";

    private static final Pattern BEARER = Pattern.compile(
            "(?i)(bearer\\s+)[a-z0-9._~+/=-]+"
    );

    private static final Pattern NAMED_SECRET = Pattern.compile(
            "(?i)(\\\"?(?:api[-_]?key|token|secret|password)\\\"?" +
                    "\\s*[:=]\\s*\\\"?)[^\\\",\\s}]+"
    );

    private final String configuredApiKey;

    public SecretRedactor(AgentModelProperties properties) {
        this.configuredApiKey = properties.apiKey();
    }

    public String redact(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        String sanitized = value;
        if (!configuredApiKey.isBlank()) {
            sanitized = sanitized.replace(configuredApiKey, REDACTED);
        }

        sanitized = BEARER.matcher(sanitized)
                .replaceAll("$1" + REDACTED);
        return NAMED_SECRET.matcher(sanitized)
                .replaceAll("$1" + REDACTED);
    }
}
