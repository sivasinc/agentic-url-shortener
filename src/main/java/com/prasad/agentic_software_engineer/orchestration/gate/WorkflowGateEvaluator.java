package com.prasad.agentic_software_engineer.orchestration.gate;

import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import com.prasad.agentic_software_engineer.orchestration.domain.GateDefinition;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowTask;
import org.springframework.stereotype.Component;

@Component
public class WorkflowGateEvaluator {

    public boolean evaluate(
            EngineeringWorkflow workflow,
            WorkflowTask task,
            GateDefinition gate
    ) {
        return switch (gate.type()) {
            case NONE -> true;

            case DEPENDENCIES_SUCCEEDED ->
                    dependenciesSucceeded(workflow, task);

            case CONTEXT_KEYS_PRESENT ->
                    gate.requiredContextKeys()
                            .stream()
                            .allMatch(workflow.getContext()::contains);

            case HUMAN_APPROVAL ->
                    workflow.getContext().contains(
                            approvalContextKey(
                                    task,
                                    workflow.getRevision()
                            )
                    );
        };
    }

    public boolean dependenciesSucceeded(
            EngineeringWorkflow workflow,
            WorkflowTask task
    ) {
        return task.getDependencyIds()
                .stream()
                .map(workflow::requireTask)
                .allMatch(WorkflowTask::isSucceeded);
    }

    public String approvalContextKey(
            WorkflowTask task,
            long revision
    ) {
        return "approval." +
                task.getId() +
                ".revision." +
                revision;
    }
}