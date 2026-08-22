package com.prasad.agentic_software_engineer.orchestration.engine;

import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowTask;
import org.springframework.stereotype.Component;

@Component
public class ReleaseReadinessTaskHandler
        implements WorkflowTaskHandler {

    @Override
    public TaskType supports() {
        return TaskType.RELEASE_READINESS;
    }

    @Override
    public TaskExecutionResult execute(
            EngineeringWorkflow workflow,
            WorkflowTask task
    ) {
        return TaskExecutionResult.empty();
    }
}
