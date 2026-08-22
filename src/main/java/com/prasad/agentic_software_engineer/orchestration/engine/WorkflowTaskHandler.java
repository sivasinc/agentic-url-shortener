package com.prasad.agentic_software_engineer.orchestration.engine;

import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowTask;

public interface WorkflowTaskHandler {

    TaskType supports();

    TaskExecutionResult execute(
            EngineeringWorkflow workflow,
            WorkflowTask task
    ) throws Exception;
}