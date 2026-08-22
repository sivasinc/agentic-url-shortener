package com.prasad.agentic_software_engineer.orchestration.engine;

import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class WorkflowTaskHandlerRegistry {

    private final Map<TaskType, WorkflowTaskHandler> handlers;

    public WorkflowTaskHandlerRegistry(
            List<WorkflowTaskHandler> handlers
    ) {
        Map<TaskType, WorkflowTaskHandler> indexed =
                new EnumMap<>(TaskType.class);

        for (WorkflowTaskHandler handler : handlers) {
            WorkflowTaskHandler previous =
                    indexed.putIfAbsent(
                            handler.supports(),
                            handler
                    );

            if (previous != null) {
                throw new IllegalStateException(
                        "Multiple task handlers registered for " +
                                handler.supports()
                );
            }
        }

        this.handlers = Map.copyOf(indexed);
    }

    public WorkflowTaskHandler require(TaskType taskType) {
        WorkflowTaskHandler handler = handlers.get(taskType);

        if (handler == null) {
            throw new IllegalStateException(
                    "No task handler registered for " + taskType
            );
        }

        return handler;
    }
}