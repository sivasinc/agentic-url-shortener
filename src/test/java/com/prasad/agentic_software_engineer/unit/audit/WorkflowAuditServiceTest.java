package com.prasad.agentic_software_engineer.unit.audit;

import com.prasad.agentic_software_engineer.audit.AuditEventType;
import com.prasad.agentic_software_engineer.audit.InMemoryAuditTrailRepository;
import com.prasad.agentic_software_engineer.audit.SecretRedactor;
import com.prasad.agentic_software_engineer.audit.WorkflowAuditService;
import com.prasad.agentic_software_engineer.config.AgentModelProperties;
import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import com.prasad.agentic_software_engineer.orchestration.domain.GateDefinition;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowTask;
import com.prasad.agentic_software_engineer.orchestration.engine.AgenticContextKeys;
import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowAuditServiceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-22T12:00:00Z");

    @TempDir
    Path temporaryDirectory;

    @Test
    void storesSanitizedAgentEvidenceAndMetrics() throws Exception {
        AgentModelProperties properties = properties("super-secret-key");
        InMemoryAuditTrailRepository repository =
                new InMemoryAuditTrailRepository();
        SimpleMeterRegistry metrics = new SimpleMeterRegistry();
        WorkflowAuditService audit = new WorkflowAuditService(
                repository,
                new SecretRedactor(properties),
                new ObjectMapper(),
                metrics,
                properties
        );

        EngineeringWorkflow workflow = workflow();
        WorkflowTask task = workflow.getTasks().iterator().next();
        workflow.start(NOW);
        task.start(NOW);

        audit.taskStarted(workflow, task, NOW);
        task.succeed(NOW.plusSeconds(2));
        audit.taskSucceeded(
                workflow,
                task,
                Map.of(
                        "result", "generated",
                        "apiKey", "super-secret-key",
                        "authorization", "Bearer abc.def.ghi"
                ),
                NOW.plusSeconds(2)
        );

        assertThat(audit.events(workflow.getId()))
                .extracting(event -> event.type())
                .containsExactly(
                        AuditEventType.TASK_STARTED,
                        AuditEventType.AGENT_OUTPUT_GENERATED,
                        AuditEventType.TASK_SUCCEEDED
                );

        assertThat(audit.evidence(workflow.getId())).hasSize(1);
        String content = audit.evidence(workflow.getId()).getFirst().content();
        assertThat(content)
                .contains("[REDACTED]")
                .doesNotContain("super-secret-key")
                .doesNotContain("abc.def.ghi");

        Path artifact = temporaryDirectory.resolve(
                audit.evidence(workflow.getId()).getFirst().artifact()
        );
        assertThat(Files.readString(artifact))
                .doesNotContain("super-secret-key");

        assertThat(metrics.get("agentic.model.invocations")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    void redactsNamedSecretsAndBearerCredentials() {
        SecretRedactor redactor = new SecretRedactor(properties("configured"));

        String sanitized = redactor.redact(
                "password=hunter2 Authorization: Bearer live-token configured"
        );

        assertThat(sanitized)
                .contains("[REDACTED]")
                .doesNotContain("hunter2")
                .doesNotContain("live-token")
                .doesNotContain("configured");
    }

    private EngineeringWorkflow workflow() throws Exception {
        UUID workflowId = UUID.randomUUID();
        EngineeringWorkflow workflow = new EngineeringWorkflow(
                workflowId,
                "Add redirect analytics",
                NOW
        );

        Path artifacts = Files.createDirectories(
                temporaryDirectory.resolve("artifacts")
        );
        Path repository = Files.createDirectories(
                temporaryDirectory.resolve("repository")
        );
        Path baseline = Files.createDirectories(
                temporaryDirectory.resolve("snapshots/baseline")
        );
        Path logs = Files.createDirectories(
                temporaryDirectory.resolve("logs")
        );

        workflow.getContext().put(
                AgenticContextKeys.WORKSPACE,
                new EngineeringWorkspace(
                        workflowId,
                        1,
                        temporaryDirectory,
                        repository,
                        baseline,
                        artifacts,
                        logs,
                        Map.of()
                ),
                workflowId,
                1,
                NOW
        );

        workflow.addTask(
                new WorkflowTask(
                        UUID.randomUUID(),
                        "Analyze requirement",
                        TaskType.REQUIREMENT_ANALYSIS,
                        Set.of(),
                        GateDefinition.none(),
                        GateDefinition.none(),
                        1
                )
        );
        return workflow;
    }

    private AgentModelProperties properties(String apiKey) {
        return new AgentModelProperties(
                "openai",
                URI.create("https://api.openai.com/v1"),
                apiKey,
                "test-model",
                Duration.ofSeconds(10),
                1000
        );
    }
}
