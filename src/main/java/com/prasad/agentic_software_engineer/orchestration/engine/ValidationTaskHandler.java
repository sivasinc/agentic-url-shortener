package com.prasad.agentic_software_engineer.orchestration.engine;

import com.prasad.agentic_software_engineer.agent.repair.RepairAgent;
import com.prasad.agentic_software_engineer.config.AgentExecutionProperties;
import com.prasad.agentic_software_engineer.model.EngineeringPlan;
import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.RepositoryContext;
import com.prasad.agentic_software_engineer.model.ValidationFailure;
import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowTask;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowStatus;
import com.prasad.agentic_software_engineer.patch.AppliedPatch;
import com.prasad.agentic_software_engineer.patch.PatchService;
import com.prasad.agentic_software_engineer.validation.BuildValidationResult;
import com.prasad.agentic_software_engineer.validation.MavenBuildTool;
import com.prasad.agentic_software_engineer.validation.ValidationExhaustedException;
import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import com.prasad.agentic_software_engineer.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ValidationTaskHandler
        implements WorkflowTaskHandler {

    private final MavenBuildTool mavenBuildTool;
    private final RepairAgent repairAgent;
    private final PatchService patchService;
    private final WorkspaceService workspaceService;
    private final AgentExecutionProperties properties;

    @Override
    public TaskType supports() {
        return TaskType.VALIDATION;
    }

    @Override
    public TaskExecutionResult execute(
            EngineeringWorkflow workflow,
            WorkflowTask task
    ) {
        EngineeringWorkspace workspace =
                require(
                        workflow,
                        AgenticContextKeys.WORKSPACE,
                        EngineeringWorkspace.class
                );

        EngineeringPlan plan =
                require(
                        workflow,
                        AgenticContextKeys.ENGINEERING_PLAN,
                        EngineeringPlan.class
                );

        RepositoryContext repository =
                require(
                        workflow,
                        AgenticContextKeys.REPOSITORY_CONTEXT,
                        RepositoryContext.class
                );

        PatchProposal implementation =
                require(
                        workflow,
                        AgenticContextKeys.IMPLEMENTATION_PATCH,
                        PatchProposal.class
                );

        PatchProposal tests =
                require(
                        workflow,
                        AgenticContextKeys.TEST_PATCH,
                        PatchProposal.class
                );

        AppliedPatch appliedPatch =
                require(
                        workflow,
                        AgenticContextKeys.APPLIED_PATCH,
                        AppliedPatch.class
                );

        PatchProposal currentPatch =
                combine(implementation, tests);

        BuildValidationResult lastResult = null;

        for (int attempt = 1;
             attempt <= properties.maxAttempts();
             attempt++) {
            ensureNotSafelyStopped(workflow, workspace);

            lastResult = mavenBuildTool.validate(
                    workspace,
                    attempt
            );

            ensureNotSafelyStopped(workflow, workspace);

            if (lastResult.successful()) {
                return new TaskExecutionResult(
                        Map.of(
                                AgenticContextKeys
                                        .VALIDATION_RESULT,
                                lastResult,
                                AgenticContextKeys
                                        .APPLIED_PATCH,
                                appliedPatch
                        )
                );
            }

            if (attempt ==
                    properties.maxAttempts()) {
                break;
            }

            ValidationFailure failure =
                    lastResult.toFailure();

            PatchProposal repairedPatch =
                    repairAgent.repair(
                            plan,
                            currentPatch,
                            failure,
                            repository
                    );

            ensureNotSafelyStopped(workflow, workspace);

            workspaceService.rollback(workspace);

            appliedPatch = patchService.apply(
                    workspace,
                    repairedPatch
            );

            currentPatch = repairedPatch;

            workflow.getContext().put(
                    AgenticContextKeys.REPAIR_PATCH,
                    repairedPatch,
                    task.getId(),
                    workflow.getRevision(),
                    java.time.Instant.now()
            );
        }

        workspaceService.rollback(workspace);

        if (!workspaceService.isClean(workspace)) {
            throw new ValidationExhaustedException(
                    "Validation failed and rollback verification failed"
            );
        }

        String detail = lastResult == null
                ? "No Maven validation result was produced"
                : lastResult.toFailure().summary();

        throw new ValidationExhaustedException(
                "Validation failed after " +
                        properties.maxAttempts() +
                        " attempts; workspace was restored: " +
                        detail
        );
    }

    private void ensureNotSafelyStopped(
            EngineeringWorkflow workflow,
            EngineeringWorkspace workspace
    ) {
        if (workflow.getStatus() != WorkflowStatus.SAFE_STOPPED) {
            return;
        }

        workspaceService.rollback(workspace);
        throw new IllegalStateException(
                "Validation interrupted by workflow safe stop"
        );
    }

    private PatchProposal combine(
            PatchProposal implementation,
            PatchProposal tests
    ) {
        java.util.List<com.prasad.agentic_software_engineer.model.ProposedFileChange>
                changes =
                new java.util.ArrayList<>();

        changes.addAll(implementation.changes());
        changes.addAll(tests.changes());

        java.util.List<String> assumptions =
                new java.util.ArrayList<>();

        assumptions.addAll(
                implementation.assumptions()
        );
        assumptions.addAll(tests.assumptions());

        java.util.List<String> risks =
                new java.util.ArrayList<>();

        risks.addAll(implementation.risks());
        risks.addAll(tests.risks());

        return new PatchProposal(
                implementation.summary() +
                        "; " +
                        tests.summary(),
                changes,
                assumptions,
                risks
        );
    }

    private <T> T require(
            EngineeringWorkflow workflow,
            String key,
            Class<T> type
    ) {
        Object value = workflow
                .getContext()
                .requireValue(key);

        if (!type.isInstance(value)) {
            throw new IllegalStateException(
                    "Context key " +
                            key +
                            " is not " +
                            type.getSimpleName()
            );
        }

        return type.cast(value);
    }
}
