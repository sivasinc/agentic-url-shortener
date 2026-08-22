package com.prasad.agentic_software_engineer.audit;

import com.prasad.agentic_software_engineer.config.AgentModelProperties;
import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowTask;
import com.prasad.agentic_software_engineer.orchestration.engine.AgenticContextKeys;
import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowAuditService {

    private final AuditTrailRepository repository;
    private final SecretRedactor redactor;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;
    private final AgentModelProperties modelProperties;

    public void record(
            EngineeringWorkflow workflow,
            AuditEventType type,
            String actor,
            String detail,
            Instant occurredAt
    ) {
        append(workflow, null, type, actor, detail, null, occurredAt);
    }

    public void taskStarted(
            EngineeringWorkflow workflow,
            WorkflowTask task,
            Instant occurredAt
    ) {
        append(
                workflow,
                task,
                AuditEventType.TASK_STARTED,
                agentActor(task.getType()),
                "Task execution started",
                null,
                occurredAt
        );
    }

    public void taskSucceeded(
            EngineeringWorkflow workflow,
            WorkflowTask task,
            Map<String, Object> outputs,
            Instant occurredAt
    ) {
        AuditEvidence evidence = outputs.isEmpty()
                ? null
                : createEvidence(workflow, task, outputs, occurredAt);

        if (evidence != null) {
            append(
                    workflow,
                    task,
                    AuditEventType.AGENT_OUTPUT_GENERATED,
                    agentActor(task.getType()),
                    "Structured task output generated and stored",
                    evidence,
                    occurredAt
            );
        }

        append(
                workflow,
                task,
                AuditEventType.TASK_SUCCEEDED,
                agentActor(task.getType()),
                "Task execution succeeded",
                null,
                occurredAt
        );
        recordTaskMetrics(task, "succeeded", occurredAt);
    }

    public void taskFailed(
            EngineeringWorkflow workflow,
            WorkflowTask task,
            String detail,
            Instant occurredAt
    ) {
        append(
                workflow,
                task,
                AuditEventType.TASK_FAILED,
                agentActor(task.getType()),
                redactor.redact(detail),
                null,
                occurredAt
        );
        recordTaskMetrics(task, "failed", occurredAt);
    }

    public void validationAttempt(
            EngineeringWorkflow workflow,
            int attempt,
            boolean successful,
            String logArtifact,
            Instant occurredAt
    ) {
        record(
                workflow,
                successful
                        ? AuditEventType.VALIDATION_ATTEMPT_SUCCEEDED
                        : AuditEventType.VALIDATION_ATTEMPT_FAILED,
                "VALIDATION_TOOL",
                "Maven validation attempt " + attempt +
                        (successful ? " succeeded" : " failed") +
                        "; log=" + logArtifact,
                occurredAt
        );

        Counter.builder("agentic.validation.attempts")
                .tag("outcome", successful ? "succeeded" : "failed")
                .register(meterRegistry)
                .increment();
    }

    public void repairStarted(
            EngineeringWorkflow workflow,
            int nextAttempt,
            Instant occurredAt
    ) {
        record(
                workflow,
                AuditEventType.REPAIR_STARTED,
                "REPAIR_AGENT",
                "Repair agent invoked for validation attempt " + nextAttempt,
                occurredAt
        );
        Counter.builder("agentic.repair.attempts")
                .tag("provider", modelProperties.provider())
                .register(meterRegistry)
                .increment();
        Counter.builder("agentic.model.invocations")
                .tag("agent", "repair")
                .tag("provider", modelProperties.provider())
                .tag("outcome", "started")
                .register(meterRegistry)
                .increment();
    }

    public List<AgentAuditEvent> events(UUID workflowId) {
        return repository.findByWorkflowId(workflowId);
    }

    public List<AuditEvidence> evidence(UUID workflowId) {
        return repository.findEvidenceByWorkflowId(workflowId);
    }

    private void append(
            EngineeringWorkflow workflow,
            WorkflowTask task,
            AuditEventType type,
            String actor,
            String detail,
            AuditEvidence evidence,
            Instant occurredAt
    ) {
        UUID eventId = evidence == null
                ? UUID.randomUUID()
                : evidence.eventId();

        AgentAuditEvent event = new AgentAuditEvent(
                eventId,
                workflow.getId(),
                workflow.getRevision(),
                type,
                actor,
                task == null ? null : task.getId(),
                task == null ? null : task.getType(),
                redactor.redact(detail),
                evidence == null ? null : evidence.artifact(),
                occurredAt
        );

        repository.append(event, evidence);
        Counter.builder("agentic.audit.events")
                .tag("type", type.name().toLowerCase())
                .register(meterRegistry)
                .increment();

        if (isWorkflowOutcome(type)) {
            Counter.builder("agentic.workflow.outcomes")
                    .tag("outcome", type.name().toLowerCase())
                    .tag("provider", modelProperties.provider())
                    .register(meterRegistry)
                    .increment();

            if (workflow.getStartedAt() != null) {
                Timer.builder("agentic.workflow.duration")
                        .tag("outcome", type.name().toLowerCase())
                        .register(meterRegistry)
                        .record(Duration.between(
                                workflow.getStartedAt(),
                                occurredAt
                        ));
            }
        }
    }

    private AuditEvidence createEvidence(
            EngineeringWorkflow workflow,
            WorkflowTask task,
            Map<String, Object> outputs,
            Instant occurredAt
    ) {
        EngineeringWorkspace workspace = workflow.getContext()
                .find(AgenticContextKeys.WORKSPACE)
                .map(entry -> entry.value())
                .filter(EngineeringWorkspace.class::isInstance)
                .map(EngineeringWorkspace.class::cast)
                .orElse(null);

        if (workspace == null) {
            return null;
        }

        UUID eventId = UUID.randomUUID();
        String fileName = occurredAt.toEpochMilli() + "-" +
                task.getType().name().toLowerCase() + "-" +
                eventId + ".json";
        Path directory = workspace.artifacts().resolve("audit");
        Path file = directory.resolve(fileName).normalize();

        if (!file.startsWith(workspace.artifacts().toAbsolutePath().normalize())) {
            throw new IllegalStateException("Audit artifact escaped workspace");
        }

        String content;
        try {
            content = redactor.redact(
                    objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(
                                    evidencePayload(
                                            workflow,
                                            task,
                                            outputs
                                    )
                            )
            );
            Files.createDirectories(directory);
            Files.writeString(
                    file,
                    content,
                    StandardCharsets.UTF_8
            );
        } catch (JacksonException exception) {
            content = redactor.redact(outputs.toString());
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to write sanitized audit evidence",
                    exception
            );
        }

        String artifact = workspace.root()
                .relativize(file)
                .toString()
                .replace('\\', '/');

        return new AuditEvidence(
                eventId,
                artifact,
                "application/json",
                content
        );
    }

    private Map<String, Object> evidencePayload(
            EngineeringWorkflow workflow,
            WorkflowTask task,
            Map<String, Object> outputs
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("agent", agentActor(task.getType()));
        payload.put("provider", modelProperties.provider());
        payload.put(
                "model",
                "deterministic".equalsIgnoreCase(modelProperties.provider())
                        ? "deterministic-fixture-v1"
                        : modelProperties.model()
        );
        payload.put("workflowRevision", workflow.getRevision());

        Map<String, Object> request = new LinkedHashMap<>();
        for (String key : inputKeys(task.getType())) {
            workflow.getContext().find(key)
                    .ifPresent(entry -> request.put(key, entry.value()));
        }
        payload.put("request", request);
        payload.put("response", outputs);
        return payload;
    }

    private List<String> inputKeys(TaskType type) {
        return switch (type) {
            case REQUIREMENT_ANALYSIS -> List.of(
                    AgenticContextKeys.REQUIREMENT_CONTEXT
            );
            case REPOSITORY_ANALYSIS -> List.of(
                    AgenticContextKeys.REQUIREMENT_ANALYSIS
            );
            case ARCHITECTURE -> List.of(
                    AgenticContextKeys.REPOSITORY_CONTEXT
            );
            case IMPLEMENTATION -> List.of(
                    AgenticContextKeys.ENGINEERING_PLAN,
                    AgenticContextKeys.REPOSITORY_CONTEXT
            );
            case TEST_GENERATION -> List.of(
                    AgenticContextKeys.ENGINEERING_PLAN,
                    AgenticContextKeys.IMPLEMENTATION_PATCH,
                    AgenticContextKeys.REPOSITORY_CONTEXT
            );
            case PATCH_APPLICATION -> List.of(
                    AgenticContextKeys.IMPLEMENTATION_PATCH,
                    AgenticContextKeys.TEST_PATCH
            );
            case VALIDATION -> List.of(
                    AgenticContextKeys.APPLIED_PATCH
            );
            case DOCUMENTATION -> List.of(
                    AgenticContextKeys.REQUIREMENT_ANALYSIS,
                    AgenticContextKeys.ENGINEERING_PLAN,
                    AgenticContextKeys.APPLIED_PATCH,
                    AgenticContextKeys.VALIDATION_RESULT
            );
            default -> List.of();
        };
    }

    private void recordTaskMetrics(
            WorkflowTask task,
            String outcome,
            Instant completedAt
    ) {
        Counter.builder("agentic.task.executions")
                .tag("task", task.getType().name().toLowerCase())
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();

        if (isModelTask(task.getType())) {
            Counter.builder("agentic.model.invocations")
                    .tag("agent", task.getType().name().toLowerCase())
                    .tag("provider", modelProperties.provider())
                    .tag("outcome", outcome)
                    .register(meterRegistry)
                    .increment();
        }

        if (task.getStartedAt() != null) {
            Timer.builder("agentic.task.duration")
                    .tag("task", task.getType().name().toLowerCase())
                    .tag("outcome", outcome)
                    .register(meterRegistry)
                    .record(Duration.between(task.getStartedAt(), completedAt));
        }
    }

    private boolean isModelTask(TaskType type) {
        return type == TaskType.REQUIREMENT_ANALYSIS ||
                type == TaskType.ARCHITECTURE ||
                type == TaskType.IMPLEMENTATION ||
                type == TaskType.TEST_GENERATION ||
                type == TaskType.REPAIR ||
                type == TaskType.DOCUMENTATION;
    }

    private boolean isWorkflowOutcome(AuditEventType type) {
        return type == AuditEventType.WORKFLOW_COMPLETED ||
                type == AuditEventType.WORKFLOW_FAILED ||
                type == AuditEventType.RELEASE_REJECTED ||
                type == AuditEventType.SAFE_STOPPED;
    }

    private String agentActor(TaskType type) {
        return switch (type) {
            case REQUIREMENT_ANALYSIS -> "REQUIREMENT_AGENT";
            case REPOSITORY_ANALYSIS -> "REPOSITORY_AGENT";
            case ARCHITECTURE -> "ARCHITECTURE_AGENT";
            case IMPLEMENTATION -> "IMPLEMENTATION_AGENT";
            case TEST_GENERATION -> "TEST_AGENT";
            case PATCH_APPLICATION -> "PATCH_TOOL";
            case VALIDATION -> "VALIDATION_AGENT";
            case REPAIR -> "REPAIR_AGENT";
            case DOCUMENTATION -> "DOCUMENTATION_AGENT";
            case POLICY_EVALUATION -> "POLICY_ENGINE";
            case RELEASE_READINESS -> "RELEASE_GATE";
        };
    }
}
