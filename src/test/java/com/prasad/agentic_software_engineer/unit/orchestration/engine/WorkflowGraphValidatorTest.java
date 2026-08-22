package com.prasad.agentic_software_engineer.unit.orchestration.engine;

import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import com.prasad.agentic_software_engineer.orchestration.domain.GateDefinition;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowTask;
import com.prasad.agentic_software_engineer.orchestration.engine.WorkflowGraphValidator;
import com.prasad.agentic_software_engineer.orchestration.exception.InvalidWorkflowGraphException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowGraphValidatorTest {

    private static final Instant NOW =
            Instant.parse("2026-08-21T12:00:00Z");

    private final WorkflowGraphValidator validator =
            new WorkflowGraphValidator();

    @Test
    void acceptsValidDependencyGraph() {
        EngineeringWorkflow workflow = workflow();

        WorkflowTask requirement = task(
                TaskType.REQUIREMENT_ANALYSIS,
                Set.of()
        );

        WorkflowTask architecture = task(
                TaskType.ARCHITECTURE,
                Set.of(requirement.getId())
        );

        workflow.addTask(requirement);
        workflow.addTask(architecture);

        assertThatCode(() -> validator.validate(workflow))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingDependency() {
        EngineeringWorkflow workflow = workflow();

        workflow.addTask(
                task(
                        TaskType.IMPLEMENTATION,
                        Set.of(UUID.randomUUID())
                )
        );

        assertThatThrownBy(
                () -> validator.validate(workflow)
        )
                .isInstanceOf(
                        InvalidWorkflowGraphException.class
                )
                .hasMessageContaining(
                        "missing dependency"
                );
    }

    @Test
    void rejectsCycle() {
        EngineeringWorkflow workflow = workflow();

        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();

        WorkflowTask first = new WorkflowTask(
                firstId,
                "First",
                TaskType.ARCHITECTURE,
                Set.of(secondId),
                GateDefinition.dependenciesSucceeded(),
                GateDefinition.none(),
                1
        );

        WorkflowTask second = new WorkflowTask(
                secondId,
                "Second",
                TaskType.IMPLEMENTATION,
                Set.of(firstId),
                GateDefinition.dependenciesSucceeded(),
                GateDefinition.none(),
                1
        );

        workflow.addTask(first);
        workflow.addTask(second);

        assertThatThrownBy(
                () -> validator.validate(workflow)
        )
                .isInstanceOf(
                        InvalidWorkflowGraphException.class
                )
                .hasMessageContaining("cycle");
    }

    private EngineeringWorkflow workflow() {
        return new EngineeringWorkflow(
                UUID.randomUUID(),
                "Add redirect analytics",
                NOW
        );
    }

    private WorkflowTask task(
            TaskType type,
            Set<UUID> dependencies
    ) {
        return new WorkflowTask(
                UUID.randomUUID(),
                type.name(),
                type,
                dependencies,
                GateDefinition.dependenciesSucceeded(),
                GateDefinition.none(),
                1
        );
    }
}