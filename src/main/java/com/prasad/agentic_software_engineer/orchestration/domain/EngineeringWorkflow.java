package com.prasad.agentic_software_engineer.orchestration.domain;

import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Getter
public class EngineeringWorkflow {

    private final UUID id;
    private final String requirement;
    private final Instant createdAt;
    private final WorkflowContext context = new WorkflowContext();

    private final Map<UUID, WorkflowTask> tasks =
            new LinkedHashMap<>();

    private long revision = 1;
    private WorkflowStatus status = WorkflowStatus.CREATED;
    private Instant startedAt;
    private Instant completedAt;
    private String failureMessage;

    public EngineeringWorkflow(
            UUID id,
            String requirement,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.requirement = requireText(requirement);
        this.createdAt = Objects.requireNonNull(createdAt);
    }

    public synchronized void addTask(WorkflowTask task) {
        Objects.requireNonNull(task);

        if (status != WorkflowStatus.CREATED) {
            throw new IllegalStateException(
                    "Tasks can only be added before execution starts"
            );
        }

        if (tasks.putIfAbsent(task.getId(), task) != null) {
            throw new IllegalArgumentException(
                    "Duplicate workflow task ID: " + task.getId()
            );
        }
    }

    public synchronized Collection<WorkflowTask> getTasks() {
        return List.copyOf(tasks.values());
    }

    public synchronized Optional<WorkflowTask> findTask(
            UUID taskId
    ) {
        return Optional.ofNullable(tasks.get(taskId));
    }

    public synchronized WorkflowTask requireTask(UUID taskId) {
        WorkflowTask task = tasks.get(taskId);

        if (task == null) {
            throw new IllegalArgumentException(
                    "Unknown workflow task: " + taskId
            );
        }

        return task;
    }

    public synchronized List<WorkflowTask> pendingTasks() {
        return tasks.values()
                .stream()
                .filter(WorkflowTask::isPending)
                .sorted(
                        Comparator.comparing(
                                task -> task.getId().toString()
                        )
                )
                .toList();
    }

    public synchronized boolean allTasksSucceeded() {
        return !tasks.isEmpty() &&
                tasks.values()
                        .stream()
                        .allMatch(WorkflowTask::isSucceeded);
    }

    public synchronized void start(Instant startedAt) {
        if (status != WorkflowStatus.CREATED) {
            throw new IllegalStateException(
                    "Only a created workflow can start"
            );
        }

        if (tasks.isEmpty()) {
            throw new IllegalStateException(
                    "Workflow must contain at least one task"
            );
        }

        this.status = WorkflowStatus.RUNNING;
        this.startedAt = Objects.requireNonNull(startedAt);
    }

    public synchronized void awaitClarification() {
        if (status != WorkflowStatus.RUNNING) {
            throw new IllegalStateException(
                    "Only a running workflow can await clarification"
            );
        }

        this.status =
                WorkflowStatus.AWAITING_CLARIFICATION;
    }

    public synchronized void awaitApproval() {
        if (status != WorkflowStatus.RUNNING) {
            throw new IllegalStateException(
                    "Only a running workflow can await approval"
            );
        }

        this.status = WorkflowStatus.AWAITING_APPROVAL;
    }

    public synchronized void prepareRevision() {
        if (status != WorkflowStatus.AWAITING_CLARIFICATION) {
            throw new IllegalStateException(
                    "Only a workflow awaiting clarification can be revised"
            );
        }

        revision++;
        tasks.clear();
        context.clear();
        status = WorkflowStatus.CREATED;
        startedAt = null;
        completedAt = null;
        failureMessage = null;
    }

    public synchronized void resumeAfterApproval() {
        if (status != WorkflowStatus.AWAITING_APPROVAL) {
            throw new IllegalStateException(
                    "Only a workflow awaiting approval can resume"
            );
        }

        status = WorkflowStatus.RUNNING;
    }

    public synchronized void reject(
            String reason,
            Instant rejectedAt
    ) {
        if (status != WorkflowStatus.AWAITING_APPROVAL) {
            throw new IllegalStateException(
                    "Only a workflow awaiting approval can be rejected"
            );
        }

        tasks.values()
                .stream()
                .filter(task -> !task.isSucceeded())
                .forEach(task -> task.cancel(reason, rejectedAt));

        status = WorkflowStatus.REJECTED;
        failureMessage = requireText(reason);
        completedAt = Objects.requireNonNull(rejectedAt);
    }

    public synchronized void complete(Instant completedAt) {
        if (status != WorkflowStatus.RUNNING) {
            throw new IllegalStateException(
                    "Only a running workflow can complete"
            );
        }

        if (!allTasksSucceeded()) {
            throw new IllegalStateException(
                    "Workflow cannot complete with unfinished tasks"
            );
        }

        this.status = WorkflowStatus.COMPLETED;
        this.completedAt = Objects.requireNonNull(completedAt);
        this.failureMessage = null;
    }

    public synchronized void fail(
            String failureMessage,
            Instant completedAt
    ) {
        if (status != WorkflowStatus.RUNNING) {
            return;
        }

        this.status = WorkflowStatus.FAILED;
        this.failureMessage = requireText(failureMessage);
        this.completedAt = Objects.requireNonNull(completedAt);
    }

    public synchronized void safeStop(
            String reason,
            Instant stoppedAt
    ) {
        if (isTerminal()) {
            return;
        }

        tasks.values()
                .stream()
                .forEach(task -> task.cancel(reason, stoppedAt));

        this.status = WorkflowStatus.SAFE_STOPPED;
        this.failureMessage = requireText(reason);
        this.completedAt = Objects.requireNonNull(stoppedAt);
    }

    public synchronized boolean isTerminal() {
        return status == WorkflowStatus.COMPLETED ||
                status == WorkflowStatus.FAILED ||
                status == WorkflowStatus.REJECTED ||
                status == WorkflowStatus.SAFE_STOPPED;
    }

    private static String requireText(String value) {
        Objects.requireNonNull(value);

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    "Value cannot be blank"
            );
        }

        return value.trim();
    }
}
