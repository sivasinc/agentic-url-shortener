package com.prasad.agentic_software_engineer.orchestration.engine;

import com.prasad.agentic_software_engineer.audit.AuditEventType;
import com.prasad.agentic_software_engineer.audit.WorkflowAuditService;
import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import com.prasad.agentic_software_engineer.orchestration.domain.GateType;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowStatus;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowTask;
import com.prasad.agentic_software_engineer.orchestration.exception.WorkflowGateRejectedException;
import com.prasad.agentic_software_engineer.orchestration.gate.WorkflowGateEvaluator;
import com.prasad.agentic_software_engineer.orchestration.repository.WorkflowRepository;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class WorkflowEngine implements AutoCloseable {

    private final WorkflowGraphValidator graphValidator;
    private final WorkflowGateEvaluator gateEvaluator;
    private final WorkflowTaskHandlerRegistry handlerRegistry;
    private final WorkflowRepository workflowRepository;
    private final WorkflowAuditService auditService;
    private final Clock clock;

    private final ExecutorService executor =
            Executors.newVirtualThreadPerTaskExecutor();

    public WorkflowEngine(
            WorkflowGraphValidator graphValidator,
            WorkflowGateEvaluator gateEvaluator,
            WorkflowTaskHandlerRegistry handlerRegistry,
            WorkflowRepository workflowRepository,
            WorkflowAuditService auditService,
            Clock clock
    ) {
        this.graphValidator = graphValidator;
        this.gateEvaluator = gateEvaluator;
        this.handlerRegistry = handlerRegistry;
        this.workflowRepository = workflowRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    public EngineeringWorkflow execute(
            EngineeringWorkflow workflow
    ) {
        graphValidator.validate(workflow);

        if (workflow.getStatus() ==
                WorkflowStatus.CREATED) {
            workflow.start(clock.instant());
            auditService.record(
                    workflow,
                    AuditEventType.WORKFLOW_STARTED,
                    "ORCHESTRATOR",
                    "Workflow execution started",
                    clock.instant()
            );
        }

        workflowRepository.save(workflow);

        while (workflow.getStatus() ==
                WorkflowStatus.RUNNING) {
            List<WorkflowTask> pendingTasks =
                    workflow.pendingTasks();

            if (pendingTasks.isEmpty()) {
                if (workflow.allTasksSucceeded()) {
                    workflow.complete(clock.instant());
                    auditService.record(
                            workflow,
                            AuditEventType.WORKFLOW_COMPLETED,
                            "ORCHESTRATOR",
                            "All workflow tasks completed",
                            clock.instant()
                    );
                    workflowRepository.save(workflow);
                }

                break;
            }

            List<WorkflowTask> readyTasks =
                    pendingTasks.stream()
                            .filter(
                                    task ->
                                            gateEvaluator
                                                    .dependenciesSucceeded(
                                                            workflow,
                                                            task
                                                    )
                            )
                            .filter(
                                    task ->
                                            gateEvaluator.evaluate(
                                                    workflow,
                                                    task,
                                                    task.getEntryGate()
                                            )
                            )
                            .toList();

            if (readyTasks.isEmpty()) {
                if (isWaitingForHumanApproval(
                        workflow,
                        pendingTasks
                )) {
                    workflow.awaitApproval();
                    auditService.record(
                            workflow,
                            AuditEventType.APPROVAL_REQUIRED,
                            "POLICY_ENGINE",
                            "Release-readiness approval is required",
                            clock.instant()
                    );
                } else {
                    workflow.fail(
                            "No executable tasks remain; " +
                                    "the graph is blocked by unsatisfied gates",
                            clock.instant()
                    );
                    auditService.record(
                            workflow,
                            AuditEventType.WORKFLOW_FAILED,
                            "ORCHESTRATOR",
                            workflow.getFailureMessage(),
                            clock.instant()
                    );
                }

                workflowRepository.save(workflow);
                break;
            }

            executeParallelWave(
                    workflow,
                    readyTasks
            );

            WorkflowTask failedTask =
                    workflow.getTasks()
                            .stream()
                            .filter(WorkflowTask::isFailed)
                            .findFirst()
                            .orElse(null);

            if (failedTask != null) {
                workflow.fail(
                        "Task failed: " +
                                failedTask.getName() +
                                " - " +
                                failedTask.getFailureMessage(),
                        clock.instant()
                );

                auditService.record(
                        workflow,
                        AuditEventType.WORKFLOW_FAILED,
                        "ORCHESTRATOR",
                        workflow.getFailureMessage(),
                        clock.instant()
                );

                workflowRepository.save(workflow);
                break;
            }

            workflowRepository.save(workflow);
        }

        return workflow;
    }

    private void executeParallelWave(
            EngineeringWorkflow workflow,
            List<WorkflowTask> readyTasks
    ) {
        List<CompletableFuture<Void>> futures =
                readyTasks.stream()
                        .map(
                                task ->
                                        CompletableFuture.runAsync(
                                                () -> executeTask(
                                                        workflow,
                                                        task
                                                ),
                                                executor
                                        )
                        )
                        .toList();

        CompletableFuture.allOf(
                futures.toArray(
                        CompletableFuture[]::new
                )
        ).join();
    }

    private void executeTask(
            EngineeringWorkflow workflow,
            WorkflowTask task
    ) {
        task.start(clock.instant());

        try {
            auditService.taskStarted(workflow, task, clock.instant());

            WorkflowTaskHandler handler =
                    handlerRegistry.require(
                            task.getType()
                    );

            TaskExecutionResult result =
                    handler.execute(workflow, task);

            if (workflow.getStatus() ==
                    WorkflowStatus.SAFE_STOPPED) {
                task.cancel(
                        "Workflow was safely stopped",
                        clock.instant()
                );

                return;
            }

            Instant outputTime = clock.instant();

            result.outputs().forEach(
                    (key, value) ->
                            workflow.getContext().put(
                                    key,
                                    value,
                                    task.getId(),
                                    workflow.getRevision(),
                                    outputTime
                            )
            );

            if (!gateEvaluator.evaluate(
                    workflow,
                    task,
                    task.getExitGate()
            )) {
                throw new WorkflowGateRejectedException(
                        "Exit gate rejected task " +
                                task.getName()
                );
            }

            auditService.taskSucceeded(
                    workflow,
                    task,
                    result.outputs(),
                    clock.instant()
            );
            task.succeed(clock.instant());

            if (workflow.getStatus() ==
                    WorkflowStatus.AWAITING_CLARIFICATION) {
                auditService.record(
                        workflow,
                        AuditEventType.CLARIFICATION_REQUIRED,
                        "REQUIREMENT_AGENT",
                        "Requirement analysis requires human clarification",
                        clock.instant()
                );
            }
        } catch (Exception exception) {
            if (workflow.getStatus() ==
                    WorkflowStatus.SAFE_STOPPED) {
                task.cancel(
                        "Workflow was safely stopped",
                        clock.instant()
                );
            } else {
                task.fail(
                        safeFailureMessage(exception),
                        clock.instant()
                );
                auditService.taskFailed(
                        workflow,
                        task,
                        safeFailureMessage(exception),
                        clock.instant()
                );
            }
        }
    }

    private boolean isWaitingForHumanApproval(
            EngineeringWorkflow workflow,
            List<WorkflowTask> pendingTasks
    ) {
        return pendingTasks.stream()
                .filter(
                        task ->
                                gateEvaluator.dependenciesSucceeded(
                                        workflow,
                                        task
                                )
                )
                .anyMatch(
                        task ->
                                task.getEntryGate().type() ==
                                        GateType.HUMAN_APPROVAL
                );
    }

    private String safeFailureMessage(Exception exception) {
        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            return exception.getClass().getSimpleName();
        }

        return message;
    }

    @Override
    @PreDestroy
    public void close() {
        executor.shutdownNow();
    }
}
