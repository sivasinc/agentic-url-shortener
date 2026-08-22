package com.prasad.agentic_software_engineer.orchestration.domain;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public record GateDefinition(
        GateType type,
        Set<String> requiredContextKeys
) {

    public GateDefinition {
        type = Objects.requireNonNull(type);
        requiredContextKeys = Set.copyOf(
                Objects.requireNonNull(requiredContextKeys)
        );
    }

    public static GateDefinition none() {
        return new GateDefinition(
                GateType.NONE,
                Set.of()
        );
    }

    public static GateDefinition dependenciesSucceeded() {
        return new GateDefinition(
                GateType.DEPENDENCIES_SUCCEEDED,
                Set.of()
        );
    }

    public static GateDefinition contextKeys(
            String... requiredKeys
    ) {
        Set<String> keys = Arrays.stream(requiredKeys)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());

        if (keys.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one context key is required"
            );
        }

        return new GateDefinition(
                GateType.CONTEXT_KEYS_PRESENT,
                keys
        );
    }

    public static GateDefinition humanApproval() {
        return new GateDefinition(
                GateType.HUMAN_APPROVAL,
                Set.of()
        );
    }
}