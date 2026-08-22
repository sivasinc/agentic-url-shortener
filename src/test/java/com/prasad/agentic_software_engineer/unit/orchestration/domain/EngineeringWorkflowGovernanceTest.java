package com.prasad.agentic_software_engineer.unit.orchestration.domain;

import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import com.prasad.agentic_software_engineer.orchestration.domain.GateDefinition;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskStatus;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowStatus;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowTask;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EngineeringWorkflowGovernanceTest {

    private static final Instant NOW =
            Instant.parse("2026-08-21T12:00:00Z");

    @Test
    void clarificationCreatesFreshRevision() {
        EngineeringWorkflow workflow = workflow();
        workflow.start(NOW);
        workflow.awaitClarification();

        workflow.prepareRevision();

        assertThat(workflow.getRevision()).isEqualTo(2);
        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.CREATED);
        assertThat(workflow.getTasks()).isEmpty();
        assertThat(workflow.getContext().snapshot()).isEmpty();
    }

    @Test
    void approvalCanOnlyResumeAwaitingWorkflow() {
        EngineeringWorkflow workflow = workflow();

        assertThatThrownBy(workflow::resumeAfterApproval)
                .isInstanceOf(IllegalStateException.class);

        workflow.start(NOW);
        workflow.awaitApproval();
        workflow.resumeAfterApproval();

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.RUNNING);
    }

    @Test
    void rejectionIsTerminalAndCancelsPendingTasks() {
        EngineeringWorkflow workflow = workflow();
        workflow.start(NOW);
        workflow.awaitApproval();

        workflow.reject("Risk not accepted", NOW.plusSeconds(1));

        assertThat(workflow.getStatus()).isEqualTo(WorkflowStatus.REJECTED);
        assertThat(workflow.isTerminal()).isTrue();
        assertThat(workflow.getTasks())
                .extracting(WorkflowTask::getStatus)
                .containsOnly(TaskStatus.CANCELLED);
    }

    @Test
    void safeStopCancelsRunningAndPendingTasks() {
        EngineeringWorkflow workflow = workflow();
        WorkflowTask task = workflow.getTasks().iterator().next();
        workflow.start(NOW);
        task.start(NOW);

        workflow.safeStop("Operator requested stop", NOW.plusSeconds(1));

        assertThat(workflow.getStatus())
                .isEqualTo(WorkflowStatus.SAFE_STOPPED);
        assertThat(task.getStatus()).isEqualTo(TaskStatus.CANCELLED);
    }

    private EngineeringWorkflow workflow() {
        EngineeringWorkflow workflow = new EngineeringWorkflow(
                UUID.randomUUID(),
                "Improve analytics",
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
}
