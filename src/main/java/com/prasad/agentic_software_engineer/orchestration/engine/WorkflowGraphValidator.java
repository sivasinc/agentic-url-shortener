package com.prasad.agentic_software_engineer.orchestration.engine;

import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowTask;
import com.prasad.agentic_software_engineer.orchestration.exception.InvalidWorkflowGraphException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class WorkflowGraphValidator {

    public void validate(EngineeringWorkflow workflow) {
        if (workflow.getTasks().isEmpty()) {
            throw new InvalidWorkflowGraphException(
                    "Workflow graph must contain at least one task"
            );
        }

        validateDependenciesExist(workflow);
        validateAcyclic(workflow);
    }

    private void validateDependenciesExist(
            EngineeringWorkflow workflow
    ) {
        for (WorkflowTask task : workflow.getTasks()) {
            for (UUID dependencyId : task.getDependencyIds()) {
                if (task.getId().equals(dependencyId)) {
                    throw new InvalidWorkflowGraphException(
                            "Task cannot depend on itself: " +
                                    task.getId()
                    );
                }

                if (workflow.findTask(dependencyId).isEmpty()) {
                    throw new InvalidWorkflowGraphException(
                            "Task " + task.getId() +
                                    " has missing dependency " +
                                    dependencyId
                    );
                }
            }
        }
    }

    private void validateAcyclic(
            EngineeringWorkflow workflow
    ) {
        Map<UUID, VisitState> states = new HashMap<>();

        for (WorkflowTask task : workflow.getTasks()) {
            visit(workflow, task, states);
        }
    }

    private void visit(
            EngineeringWorkflow workflow,
            WorkflowTask task,
            Map<UUID, VisitState> states
    ) {
        VisitState state = states.get(task.getId());

        if (state == VisitState.VISITING) {
            throw new InvalidWorkflowGraphException(
                    "Workflow graph contains a cycle at task " +
                            task.getId()
            );
        }

        if (state == VisitState.VISITED) {
            return;
        }

        states.put(task.getId(), VisitState.VISITING);

        for (UUID dependencyId : task.getDependencyIds()) {
            visit(
                    workflow,
                    workflow.requireTask(dependencyId),
                    states
            );
        }

        states.put(task.getId(), VisitState.VISITED);
    }

    private enum VisitState {
        VISITING,
        VISITED
    }
}