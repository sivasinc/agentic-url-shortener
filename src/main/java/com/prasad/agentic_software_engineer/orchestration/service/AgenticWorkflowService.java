package com.prasad.agentic_software_engineer.orchestration.service;

import com.prasad.agentic_software_engineer.config.AgentModelProperties;
import com.prasad.agentic_software_engineer.model.EngineeringPlan;
import com.prasad.agentic_software_engineer.model.RequirementAnalysis;
import com.prasad.agentic_software_engineer.model.RequirementContext;
import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import com.prasad.agentic_software_engineer.orchestration.domain.GateDefinition;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowTask;
import com.prasad.agentic_software_engineer.orchestration.dto.CreateEngineeringWorkflowRequest;
import com.prasad.agentic_software_engineer.orchestration.dto.EngineeringWorkflowResponse;
import com.prasad.agentic_software_engineer.orchestration.dto.WorkflowTaskResponse;
import com.prasad.agentic_software_engineer.orchestration.engine.AgenticContextKeys;
import com.prasad.agentic_software_engineer.orchestration.engine.WorkflowEngine;
import com.prasad.agentic_software_engineer.orchestration.exception.WorkflowNotFoundException;
import com.prasad.agentic_software_engineer.orchestration.repository.WorkflowRepository;
import com.prasad.agentic_software_engineer.patch.AppliedPatch;
import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import com.prasad.agentic_software_engineer.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgenticWorkflowService {

    private final WorkspaceService workspaceService;
    private final WorkflowEngine workflowEngine;
    private final WorkflowRepository workflowRepository;
    private final AgentModelProperties modelProperties;
    private final Clock clock;

    public EngineeringWorkflowResponse create(
            CreateEngineeringWorkflowRequest request
    ) {
        UUID workflowId = UUID.randomUUID();
        Instant now = clock.instant();

        EngineeringWorkspace workspace =
                workspaceService.create(
                        workflowId,
                        1,
                        Path.of(request.repositoryPath())
                );

        EngineeringWorkflow workflow =
                new EngineeringWorkflow(
                        workflowId,
                        request.requirement(),
                        now
                );

        workflow.getContext().put(
                AgenticContextKeys.REQUIREMENT_CONTEXT,
                new RequirementContext(
                        request.scenarioType(),
                        request.requirement(),
                        List.of()
                ),
                workflowId,
                workflow.getRevision(),
                now
        );

        workflow.getContext().put(
                AgenticContextKeys.WORKSPACE,
                workspace,
                workflowId,
                workflow.getRevision(),
                now
        );

        addTasks(workflow);

        workflowEngine.execute(workflow);

        return toResponse(workflow);
    }

    public EngineeringWorkflowResponse get(
            UUID workflowId
    ) {
        EngineeringWorkflow workflow =
                workflowRepository.findById(workflowId)
                        .orElseThrow(
                                () ->
                                        new WorkflowNotFoundException(
                                                workflowId
                                        )
                        );

        return toResponse(workflow);
    }

    private void addTasks(
            EngineeringWorkflow workflow
    ) {
        WorkflowTask requirement = task(
                "Analyze requirement",
                TaskType.REQUIREMENT_ANALYSIS,
                Set.of(),
                GateDefinition.contextKeys(
                        AgenticContextKeys
                                .REQUIREMENT_ANALYSIS
                )
        );

        WorkflowTask repository = task(
                "Analyze repository",
                TaskType.REPOSITORY_ANALYSIS,
                Set.of(requirement.getId()),
                GateDefinition.contextKeys(
                        AgenticContextKeys
                                .REPOSITORY_CONTEXT
                )
        );

        WorkflowTask architecture = task(
                "Create engineering plan",
                TaskType.ARCHITECTURE,
                Set.of(repository.getId()),
                GateDefinition.contextKeys(
                        AgenticContextKeys
                                .ENGINEERING_PLAN
                )
        );

        WorkflowTask implementation = task(
                "Generate implementation",
                TaskType.IMPLEMENTATION,
                Set.of(architecture.getId()),
                GateDefinition.contextKeys(
                        AgenticContextKeys
                                .IMPLEMENTATION_PATCH
                )
        );

        WorkflowTask tests = task(
                "Generate tests",
                TaskType.TEST_GENERATION,
                Set.of(implementation.getId()),
                GateDefinition.contextKeys(
                        AgenticContextKeys.TEST_PATCH
                )
        );

        WorkflowTask patch = task(
                "Validate and apply patch",
                TaskType.PATCH_APPLICATION,
                Set.of(
                        implementation.getId(),
                        tests.getId()
                ),
                GateDefinition.contextKeys(
                        AgenticContextKeys.APPLIED_PATCH
                )
        );

        WorkflowTask validation = task(
                "Compile and validate generated change",
                TaskType.VALIDATION,
                Set.of(patch.getId()),
                GateDefinition.contextKeys(
                        AgenticContextKeys.VALIDATION_RESULT
                )
        );

        workflow.addTask(requirement);
        workflow.addTask(repository);
        workflow.addTask(architecture);
        workflow.addTask(implementation);
        workflow.addTask(tests);
        workflow.addTask(patch);
        workflow.addTask(validation);
    }

    private WorkflowTask task(
            String name,
            TaskType type,
            Set<UUID> dependencies,
            GateDefinition exitGate
    ) {
        return new WorkflowTask(
                UUID.randomUUID(),
                name,
                type,
                dependencies,
                GateDefinition.dependenciesSucceeded(),
                exitGate,
                1
        );
    }

    private EngineeringWorkflowResponse toResponse(
            EngineeringWorkflow workflow
    ) {
        RequirementAnalysis analysis =
                context(
                        workflow,
                        AgenticContextKeys
                                .REQUIREMENT_ANALYSIS,
                        RequirementAnalysis.class
                );

        EngineeringPlan plan =
                context(
                        workflow,
                        AgenticContextKeys
                                .ENGINEERING_PLAN,
                        EngineeringPlan.class
                );

        AppliedPatch patch =
                context(
                        workflow,
                        AgenticContextKeys.APPLIED_PATCH,
                        AppliedPatch.class
                );

        List<WorkflowTaskResponse> tasks =
                workflow.getTasks()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        task ->
                                                task.getId()
                                                        .toString()
                                )
                        )
                        .map(
                                task ->
                                        new WorkflowTaskResponse(
                                                task.getId(),
                                                task.getName(),
                                                task.getType(),
                                                task.getStatus(),
                                                task.getDependencyIds(),
                                                task.getAttempt(),
                                                task.getFailureMessage()
                                        )
                        )
                        .toList();

        return new EngineeringWorkflowResponse(
                workflow.getId(),
                workflow.getRevision(),
                workflow.getStatus(),
                modelProperties.provider(),
                analysis,
                plan,
                patch == null
                        ? List.of()
                        : patch.changedFiles(),
                patch == null ? null : patch.diff(),
                analysis == null
                        ? List.of()
                        : analysis.ambiguities(),
                tasks,
                workflow.getFailureMessage()
        );
    }

    private <T> T context(
            EngineeringWorkflow workflow,
            String key,
            Class<T> type
    ) {
        return workflow.getContext()
                .find(key)
                .map(entry -> entry.value())
                .filter(type::isInstance)
                .map(type::cast)
                .orElse(null);
    }
}