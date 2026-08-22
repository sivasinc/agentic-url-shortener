package com.prasad.agentic_software_engineer.orchestration.domain;

import lombok.Getter;

import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Getter
public class WorkflowTask {

    private final UUID id;
    private final String name;
    private final TaskType type;
    private final Set<UUID> dependencyIds;
    private final GateDefinition entryGate;
    private final GateDefinition exitGate;
    private final int maxAttempts;

    private TaskStatus status = TaskStatus.PENDING;
    private int attempt;
    private Instant startedAt;
    private Instant completedAt;
    private String failureMessage;

    public WorkflowTask(
            UUID id,
            String name,
            TaskType type,
            Set<UUID> dependencyIds,
            GateDefinition entryGate,
            GateDefinition exitGate,
            int maxAttempts
    ) {
        this.id = Objects.requireNonNull(id);
        this.name = requireText(name, "Task name");
        this.type = Objects.requireNonNull(type);
        this.dependencyIds = Set.copyOf(
                Objects.requireNonNull(dependencyIds)
        );
        this.entryGate = Objects.requireNonNull(entryGate);
        this.exitGate = Objects.requireNonNull(exitGate);

        if (maxAttempts < 1) {
            throw new IllegalArgumentException(
                    "Task max attempts must be at least one"
            );
        }

        this.maxAttempts = maxAttempts;
    }

    public synchronized void start(Instant startedAt) {
        if (status != TaskStatus.PENDING) {
            throw new IllegalStateException(
                    "Only a pending task can start"
            );
        }

        this.status = TaskStatus.RUNNING;
        this.attempt++;
        this.startedAt = Objects.requireNonNull(startedAt);
        this.completedAt = null;
        this.failureMessage = null;
    }

    public synchronized void succeed(Instant completedAt) {
        if (status != TaskStatus.RUNNING) {
            throw new IllegalStateException(
                    "Only a running task can succeed"
            );
        }

        this.status = TaskStatus.SUCCEEDED;
        this.completedAt = Objects.requireNonNull(completedAt);
        this.failureMessage = null;
    }

    public synchronized void fail(
            String failureMessage,
            Instant completedAt
    ) {
        if (status != TaskStatus.RUNNING) {
            throw new IllegalStateException(
                    "Only a running task can fail"
            );
        }

        this.status = TaskStatus.FAILED;
        this.failureMessage = requireText(
                failureMessage,
                "Failure message"
        );
        this.completedAt = Objects.requireNonNull(completedAt);
    }

    public synchronized void cancel(
            String reason,
            Instant completedAt
    ) {
        if (status != TaskStatus.PENDING &&
                status != TaskStatus.RUNNING) {
            return;
        }

        this.status = TaskStatus.CANCELLED;
        this.failureMessage = requireText(
                reason,
                "Cancellation reason"
        );
        this.completedAt = Objects.requireNonNull(completedAt);
    }

    public boolean isPending() {
        return status == TaskStatus.PENDING;
    }

    public boolean isSucceeded() {
        return status == TaskStatus.SUCCEEDED;
    }

    public boolean isFailed() {
        return status == TaskStatus.FAILED;
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
