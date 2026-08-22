package com.prasad.agentic_software_engineer.orchestration.service;

import com.prasad.agentic_software_engineer.audit.AuditEventType;
import com.prasad.agentic_software_engineer.audit.WorkflowAuditService;
import com.prasad.agentic_software_engineer.config.AgentModelProperties;
import com.prasad.agentic_software_engineer.model.EngineeringPlan;
import com.prasad.agentic_software_engineer.model.RequirementAnalysis;
import com.prasad.agentic_software_engineer.model.RequirementContext;
import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import com.prasad.agentic_software_engineer.orchestration.domain.GateDefinition;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowStatus;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowTask;
import com.prasad.agentic_software_engineer.orchestration.dto.CreateEngineeringWorkflowRequest;
import com.prasad.agentic_software_engineer.orchestration.dto.EngineeringWorkflowResponse;
import com.prasad.agentic_software_engineer.orchestration.dto.WorkflowTaskResponse;
import com.prasad.agentic_software_engineer.orchestration.engine.AgenticContextKeys;
import com.prasad.agentic_software_engineer.orchestration.engine.WorkflowEngine;
import com.prasad.agentic_software_engineer.orchestration.exception.InvalidWorkflowTransitionException;
import com.prasad.agentic_software_engineer.orchestration.exception.WorkflowNotFoundException;
import com.prasad.agentic_software_engineer.orchestration.gate.WorkflowGateEvaluator;
import com.prasad.agentic_software_engineer.orchestration.governance.GovernanceDecision;
import com.prasad.agentic_software_engineer.orchestration.repository.WorkflowRepository;
import com.prasad.agentic_software_engineer.patch.AppliedPatch;
import com.prasad.agentic_software_engineer.validation.MavenBuildTool;
import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import com.prasad.agentic_software_engineer.workspace.WorkspaceService;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class AgenticWorkflowService
        implements AutoCloseable {

    private static final int MAX_CONCURRENT_WORKFLOWS = 4;

    private final WorkspaceService workspaceService;
    private final WorkflowEngine workflowEngine;
    private final WorkflowRepository workflowRepository;
    private final WorkflowGateEvaluator gateEvaluator;
    private final MavenBuildTool mavenBuildTool;
    private final WorkflowAuditService auditService;
    private final AgentModelProperties modelProperties;
    private final Clock clock;

    private final ExecutorService workflowExecutor =
            Executors.newFixedThreadPool(
                    MAX_CONCURRENT_WORKFLOWS,
                    Thread.ofVirtual()
                            .name("engineering-workflow-", 0)
                            .factory()
            );

    private final ConcurrentMap<UUID, CompletableFuture<Void>> executions =
            new ConcurrentHashMap<>();

    public EngineeringWorkflowResponse create(
            CreateEngineeringWorkflowRequest request
    ) {
        UUID workflowId = UUID.randomUUID();
        Instant now = clock.instant();

        EngineeringWorkspace workspace = workspaceService.create(
                workflowId,
                1,
                Path.of(request.repositoryPath())
        );

        EngineeringWorkflow workflow = new EngineeringWorkflow(
                workflowId,
                request.requirement(),
                now
        );

        initializeContext(
                workflow,
                workspace,
                request.repositoryPath(),
                new RequirementContext(
                        request.scenarioType(),
                        request.requirement(),
                        List.of()
                ),
                null
        );

        addTasks(workflow);
        workflowRepository.save(workflow);
        auditService.record(
                workflow,
                AuditEventType.WORKFLOW_CREATED,
                "API_CLIENT",
                "Engineering workflow created",
                now
        );

        EngineeringWorkflowResponse response = toResponse(workflow);
        schedule(workflow);
        return response;
    }

    public EngineeringWorkflowResponse get(UUID workflowId) {
        return toResponse(requireWorkflow(workflowId));
    }

    public EngineeringWorkflowResponse clarify(
            UUID workflowId,
            String actor,
            List<String> answers
    ) {
        EngineeringWorkflow workflow = requireWorkflow(workflowId);

        synchronized (workflow) {
            requireStatus(
                    workflow,
                    WorkflowStatus.AWAITING_CLARIFICATION,
                    "submit clarification"
            );

            boolean analysisFinished = workflow.getTasks()
                    .stream()
                    .filter(task ->
                            task.getType() == TaskType.REQUIREMENT_ANALYSIS
                    )
                    .anyMatch(WorkflowTask::isSucceeded);

            if (!analysisFinished) {
                throw new InvalidWorkflowTransitionException(
                        "Clarification can be submitted only after " +
                                "requirement analysis is finalized"
                );
            }

            RequirementContext previous = contextRequired(
                    workflow,
                    AgenticContextKeys.REQUIREMENT_CONTEXT,
                    RequirementContext.class
            );

            String repositoryPath = contextRequired(
                    workflow,
                    AgenticContextKeys.REPOSITORY_PATH,
                    String.class
            );

            List<String> combinedAnswers = new ArrayList<>(
                    previous.clarificationHistory()
            );
            combinedAnswers.addAll(answers);

            EngineeringWorkspace workspace = workspaceService.create(
                    workflowId,
                    workflow.getRevision() + 1,
                    Path.of(repositoryPath)
            );

            GovernanceDecision decision = new GovernanceDecision(
                    actor,
                    "CLARIFICATION_SUBMITTED",
                    String.join(" | ", answers),
                    clock.instant()
            );

            workflow.prepareRevision();
            initializeContext(
                    workflow,
                    workspace,
                    repositoryPath,
                    new RequirementContext(
                            previous.scenarioType(),
                            previous.rawRequirement(),
                            combinedAnswers
                    ),
                    decision
            );
            addTasks(workflow);
            workflowRepository.save(workflow);
            auditService.record(
                    workflow,
                    AuditEventType.CLARIFICATION_SUBMITTED,
                    actor,
                    "Clarification submitted; workflow revision created",
                    clock.instant()
            );
        }

        schedule(workflow);
        return toResponse(workflow);
    }

    public EngineeringWorkflowResponse decideRelease(
            UUID workflowId,
            String actor,
            boolean approved,
            String reason
    ) {
        EngineeringWorkflow workflow = requireWorkflow(workflowId);
        boolean resume = false;

        synchronized (workflow) {
            requireStatus(
                    workflow,
                    WorkflowStatus.AWAITING_APPROVAL,
                    "decide release readiness"
            );

            WorkflowTask releaseTask = workflow.getTasks()
                    .stream()
                    .filter(task -> task.getType() == TaskType.RELEASE_READINESS)
                    .findFirst()
                    .orElseThrow(
                            () -> new InvalidWorkflowTransitionException(
                                    "Workflow has no release-readiness task"
                            )
                    );

            GovernanceDecision decision = new GovernanceDecision(
                    actor,
                    approved ? "RELEASE_APPROVED" : "RELEASE_REJECTED",
                    reason,
                    clock.instant()
            );

            workflow.getContext().put(
                    AgenticContextKeys.RELEASE_DECISION,
                    decision,
                    releaseTask.getId(),
                    workflow.getRevision(),
                    clock.instant()
            );

            if (approved) {
                workflow.getContext().put(
                        gateEvaluator.approvalContextKey(
                                releaseTask,
                                workflow.getRevision()
                        ),
                        decision,
                        releaseTask.getId(),
                        workflow.getRevision(),
                        clock.instant()
                );
                workflow.resumeAfterApproval();
                auditService.record(
                        workflow,
                        AuditEventType.RELEASE_APPROVED,
                        actor,
                        reason,
                        clock.instant()
                );
                resume = true;
            } else {
                rollbackCurrentWorkspace(workflow);
                workflow.reject(reason, clock.instant());
                auditService.record(
                        workflow,
                        AuditEventType.RELEASE_REJECTED,
                        actor,
                        reason,
                        clock.instant()
                );
            }

            workflowRepository.save(workflow);
        }

        if (resume) {
            schedule(workflow);
        }
        return toResponse(workflow);
    }

    public EngineeringWorkflowResponse safeStop(
            UUID workflowId,
            String actor,
            String reason
    ) {
        EngineeringWorkflow workflow = requireWorkflow(workflowId);

        synchronized (workflow) {
            if (workflow.isTerminal()) {
                throw new InvalidWorkflowTransitionException(
                        "Terminal workflow cannot be safely stopped"
                );
            }

            workflow.getContext().put(
                    AgenticContextKeys.SAFE_STOP_DECISION,
                    new GovernanceDecision(
                            actor,
                            "SAFE_STOP_REQUESTED",
                            reason,
                            clock.instant()
                    ),
                    workflow.getId(),
                    workflow.getRevision(),
                    clock.instant()
            );
            workflow.safeStop(reason, clock.instant());
            auditService.record(
                    workflow,
                    AuditEventType.SAFE_STOPPED,
                    actor,
                    reason,
                    clock.instant()
            );
            workflowRepository.save(workflow);
        }

        mavenBuildTool.cancel(workflowId);
        CompletableFuture<Void> execution = executions.remove(workflowId);
        if (execution != null) {
            execution.cancel(true);
        }

        rollbackCurrentWorkspace(workflow);
        workflowRepository.save(workflow);
        return toResponse(workflow);
    }

    private void schedule(EngineeringWorkflow workflow) {
        CompletableFuture<Void> execution = CompletableFuture.runAsync(
                () -> workflowEngine.execute(workflow),
                workflowExecutor
        );

        executions.put(workflow.getId(), execution);
        execution.whenComplete(
                (ignored, failure) -> executions.remove(
                        workflow.getId(),
                        execution
                )
        );
    }

    private void initializeContext(
            EngineeringWorkflow workflow,
            EngineeringWorkspace workspace,
            String repositoryPath,
            RequirementContext requirement,
            GovernanceDecision clarification
    ) {
        Instant now = clock.instant();
        put(workflow, AgenticContextKeys.REQUIREMENT_CONTEXT, requirement, now);
        put(workflow, AgenticContextKeys.WORKSPACE, workspace, now);
        put(workflow, AgenticContextKeys.REPOSITORY_PATH, repositoryPath, now);
        if (clarification != null) {
            put(
                    workflow,
                    AgenticContextKeys.CLARIFICATION_DECISION,
                    clarification,
                    now
            );
        }
    }

    private void put(
            EngineeringWorkflow workflow,
            String key,
            Object value,
            Instant now
    ) {
        workflow.getContext().put(
                key,
                value,
                workflow.getId(),
                workflow.getRevision(),
                now
        );
    }

    private void addTasks(EngineeringWorkflow workflow) {
        WorkflowTask requirement = task(
                "Analyze requirement",
                TaskType.REQUIREMENT_ANALYSIS,
                Set.of(),
                GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys(AgenticContextKeys.REQUIREMENT_ANALYSIS)
        );
        WorkflowTask repository = task(
                "Analyze repository",
                TaskType.REPOSITORY_ANALYSIS,
                Set.of(requirement.getId()),
                GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys(AgenticContextKeys.REPOSITORY_CONTEXT)
        );
        WorkflowTask architecture = task(
                "Create engineering plan",
                TaskType.ARCHITECTURE,
                Set.of(repository.getId()),
                GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys(AgenticContextKeys.ENGINEERING_PLAN)
        );
        WorkflowTask implementation = task(
                "Generate implementation",
                TaskType.IMPLEMENTATION,
                Set.of(architecture.getId()),
                GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys(AgenticContextKeys.IMPLEMENTATION_PATCH)
        );
        WorkflowTask tests = task(
                "Generate tests",
                TaskType.TEST_GENERATION,
                Set.of(implementation.getId()),
                GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys(AgenticContextKeys.TEST_PATCH)
        );
        WorkflowTask patch = task(
                "Validate and apply patch",
                TaskType.PATCH_APPLICATION,
                Set.of(implementation.getId(), tests.getId()),
                GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys(AgenticContextKeys.APPLIED_PATCH)
        );
        WorkflowTask validation = task(
                "Compile and validate generated change",
                TaskType.VALIDATION,
                Set.of(patch.getId()),
                GateDefinition.dependenciesSucceeded(),
                GateDefinition.contextKeys(AgenticContextKeys.VALIDATION_RESULT)
        );
        WorkflowTask release = task(
                "Approve release readiness",
                TaskType.RELEASE_READINESS,
                Set.of(validation.getId()),
                GateDefinition.humanApproval(),
                GateDefinition.none()
        );

        workflow.addTask(requirement);
        workflow.addTask(repository);
        workflow.addTask(architecture);
        workflow.addTask(implementation);
        workflow.addTask(tests);
        workflow.addTask(patch);
        workflow.addTask(validation);
        workflow.addTask(release);
    }

    private WorkflowTask task(
            String name,
            TaskType type,
            Set<UUID> dependencies,
            GateDefinition entryGate,
            GateDefinition exitGate
    ) {
        return new WorkflowTask(
                UUID.randomUUID(),
                name,
                type,
                dependencies,
                entryGate,
                exitGate,
                1
        );
    }

    private void rollbackCurrentWorkspace(EngineeringWorkflow workflow) {
        EngineeringWorkspace workspace = contextRequired(
                workflow,
                AgenticContextKeys.WORKSPACE,
                EngineeringWorkspace.class
        );
        workspaceService.rollback(workspace);
        if (!workspaceService.isClean(workspace)) {
            throw new IllegalStateException(
                    "Workspace rollback verification failed"
            );
        }
    }

    private EngineeringWorkflow requireWorkflow(UUID workflowId) {
        return workflowRepository.findById(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
    }

    private void requireStatus(
            EngineeringWorkflow workflow,
            WorkflowStatus required,
            String action
    ) {
        if (workflow.getStatus() != required) {
            throw new InvalidWorkflowTransitionException(
                    "Cannot " + action + " while workflow status is " +
                            workflow.getStatus()
            );
        }
    }

    private EngineeringWorkflowResponse toResponse(
            EngineeringWorkflow workflow
    ) {
        RequirementAnalysis analysis = context(
                workflow,
                AgenticContextKeys.REQUIREMENT_ANALYSIS,
                RequirementAnalysis.class
        );
        EngineeringPlan plan = context(
                workflow,
                AgenticContextKeys.ENGINEERING_PLAN,
                EngineeringPlan.class
        );
        AppliedPatch patch = context(
                workflow,
                AgenticContextKeys.APPLIED_PATCH,
                AppliedPatch.class
        );

        List<WorkflowTaskResponse> tasks = workflow.getTasks()
                .stream()
                .sorted(Comparator.comparing(task -> task.getId().toString()))
                .map(task -> new WorkflowTaskResponse(
                        task.getId(),
                        task.getName(),
                        task.getType(),
                        task.getStatus(),
                        task.getDependencyIds(),
                        task.getAttempt(),
                        task.getFailureMessage()
                ))
                .toList();

        return new EngineeringWorkflowResponse(
                workflow.getId(),
                workflow.getRevision(),
                workflow.getStatus(),
                modelProperties.provider(),
                analysis,
                plan,
                patch == null ? List.of() : patch.changedFiles(),
                patch == null ? null : patch.diff(),
                analysis == null ? List.of() : analysis.ambiguities(),
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

    private <T> T contextRequired(
            EngineeringWorkflow workflow,
            String key,
            Class<T> type
    ) {
        T value = context(workflow, key, type);
        if (value == null) {
            throw new InvalidWorkflowTransitionException(
                    "Workflow context is missing " + key
            );
        }
        return value;
    }

    @Override
    @PreDestroy
    public void close() {
        workflowExecutor.shutdownNow();
    }
}
