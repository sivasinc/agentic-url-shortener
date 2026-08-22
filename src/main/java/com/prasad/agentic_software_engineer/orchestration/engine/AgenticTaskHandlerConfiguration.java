package com.prasad.agentic_software_engineer.orchestration.engine;

import com.prasad.agentic_software_engineer.agent.architecture.ArchitectureAgent;
import com.prasad.agentic_software_engineer.agent.implementation.ImplementationAgent;
import com.prasad.agentic_software_engineer.agent.repository.RepositoryAnalysisAgent;
import com.prasad.agentic_software_engineer.agent.repository.RepositoryAssessment;
import com.prasad.agentic_software_engineer.agent.repository.RepositoryContextAssembler;
import com.prasad.agentic_software_engineer.agent.requirement.RequirementAgent;
import com.prasad.agentic_software_engineer.agent.testing.TestingAgent;
import com.prasad.agentic_software_engineer.model.EngineeringPlan;
import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.RepositoryContext;
import com.prasad.agentic_software_engineer.model.RequirementAnalysis;
import com.prasad.agentic_software_engineer.model.RequirementContext;
import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowTask;
import com.prasad.agentic_software_engineer.patch.AppliedPatch;
import com.prasad.agentic_software_engineer.patch.PatchProposalMerger;
import com.prasad.agentic_software_engineer.patch.PatchService;
import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class AgenticTaskHandlerConfiguration {

    @Bean
    WorkflowTaskHandler requirementTaskHandler(
            RequirementAgent requirementAgent
    ) {
        return handler(
                TaskType.REQUIREMENT_ANALYSIS,
                (workflow, task) -> {
                    RequirementContext context =
                            require(
                                    workflow,
                                    AgenticContextKeys
                                            .REQUIREMENT_CONTEXT,
                                    RequirementContext.class
                            );

                    RequirementAnalysis analysis =
                            requirementAgent.analyze(context);

                    if (analysis.requiresClarification()) {
                        workflow.awaitClarification();
                    }

                    return new TaskExecutionResult(
                            Map.of(
                                    AgenticContextKeys
                                            .REQUIREMENT_ANALYSIS,
                                    analysis
                            )
                    );
                }
        );
    }

    @Bean
    WorkflowTaskHandler repositoryTaskHandler(
            RepositoryAnalysisAgent repositoryAgent,
            RepositoryContextAssembler assembler
    ) {
        return handler(
                TaskType.REPOSITORY_ANALYSIS,
                (workflow, task) -> {
                    EngineeringWorkspace workspace =
                            require(
                                    workflow,
                                    AgenticContextKeys.WORKSPACE,
                                    EngineeringWorkspace.class
                            );

                    RequirementAnalysis requirement =
                            require(
                                    workflow,
                                    AgenticContextKeys
                                            .REQUIREMENT_ANALYSIS,
                                    RequirementAnalysis.class
                            );

                    RepositoryAssessment assessment =
                            repositoryAgent.analyze(
                                    workspace.repository(),
                                    requirement
                                            .normalizedRequirement()
                            );

                    RepositoryContext context =
                            assembler.assemble(
                                    workspace.repository(),
                                    requirement,
                                    assessment
                            );

                    return new TaskExecutionResult(
                            Map.of(
                                    AgenticContextKeys
                                            .REPOSITORY_ASSESSMENT,
                                    assessment,
                                    AgenticContextKeys
                                            .REPOSITORY_CONTEXT,
                                    context
                            )
                    );
                }
        );
    }

    @Bean
    WorkflowTaskHandler architectureTaskHandler(
            ArchitectureAgent architectureAgent
    ) {
        return handler(
                TaskType.ARCHITECTURE,
                (workflow, task) -> {
                    RepositoryContext repository =
                            require(
                                    workflow,
                                    AgenticContextKeys
                                            .REPOSITORY_CONTEXT,
                                    RepositoryContext.class
                            );

                    EngineeringPlan plan =
                            architectureAgent.plan(repository);

                    return TaskExecutionResult.of(
                            AgenticContextKeys
                                    .ENGINEERING_PLAN,
                            plan
                    );
                }
        );
    }

    @Bean
    WorkflowTaskHandler implementationTaskHandler(
            ImplementationAgent implementationAgent
    ) {
        return handler(
                TaskType.IMPLEMENTATION,
                (workflow, task) -> {
                    EngineeringPlan plan =
                            require(
                                    workflow,
                                    AgenticContextKeys
                                            .ENGINEERING_PLAN,
                                    EngineeringPlan.class
                            );

                    RepositoryContext repository =
                            require(
                                    workflow,
                                    AgenticContextKeys
                                            .REPOSITORY_CONTEXT,
                                    RepositoryContext.class
                            );

                    PatchProposal patch =
                            implementationAgent.generate(
                                    plan,
                                    repository
                            );

                    return TaskExecutionResult.of(
                            AgenticContextKeys
                                    .IMPLEMENTATION_PATCH,
                            patch
                    );
                }
        );
    }

    @Bean
    WorkflowTaskHandler testingTaskHandler(
            TestingAgent testingAgent
    ) {
        return handler(
                TaskType.TEST_GENERATION,
                (workflow, task) -> {
                    EngineeringPlan plan =
                            require(
                                    workflow,
                                    AgenticContextKeys
                                            .ENGINEERING_PLAN,
                                    EngineeringPlan.class
                            );

                    RepositoryContext repository =
                            require(
                                    workflow,
                                    AgenticContextKeys
                                            .REPOSITORY_CONTEXT,
                                    RepositoryContext.class
                            );

                    PatchProposal implementation =
                            require(
                                    workflow,
                                    AgenticContextKeys
                                            .IMPLEMENTATION_PATCH,
                                    PatchProposal.class
                            );

                    PatchProposal tests =
                            testingAgent.generate(
                                    plan,
                                    implementation,
                                    repository
                            );

                    return TaskExecutionResult.of(
                            AgenticContextKeys.TEST_PATCH,
                            tests
                    );
                }
        );
    }

    @Bean
    WorkflowTaskHandler patchApplicationTaskHandler(
            PatchProposalMerger merger,
            PatchService patchService
    ) {
        return handler(
                TaskType.PATCH_APPLICATION,
                (workflow, task) -> {
                    EngineeringWorkspace workspace =
                            require(
                                    workflow,
                                    AgenticContextKeys.WORKSPACE,
                                    EngineeringWorkspace.class
                            );

                    PatchProposal implementation =
                            require(
                                    workflow,
                                    AgenticContextKeys
                                            .IMPLEMENTATION_PATCH,
                                    PatchProposal.class
                            );

                    PatchProposal tests =
                            require(
                                    workflow,
                                    AgenticContextKeys.TEST_PATCH,
                                    PatchProposal.class
                            );

                    AppliedPatch appliedPatch =
                            patchService.apply(
                                    workspace,
                                    merger.merge(
                                            implementation,
                                            tests
                                    )
                            );

                    return TaskExecutionResult.of(
                            AgenticContextKeys.APPLIED_PATCH,
                            appliedPatch
                    );
                }
        );
    }

    private WorkflowTaskHandler handler(
            TaskType type,
            HandlerBody body
    ) {
        return new WorkflowTaskHandler() {

            @Override
            public TaskType supports() {
                return type;
            }

            @Override
            public TaskExecutionResult execute(
                    EngineeringWorkflow workflow,
                    WorkflowTask task
            ) throws Exception {
                return body.execute(
                        workflow,
                        task
                );
            }
        };
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

    @FunctionalInterface
    private interface HandlerBody {

        TaskExecutionResult execute(
                EngineeringWorkflow workflow,
                WorkflowTask task
        ) throws Exception;
    }
}