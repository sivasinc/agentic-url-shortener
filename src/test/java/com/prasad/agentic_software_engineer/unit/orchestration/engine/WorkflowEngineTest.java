package com.prasad.agentic_software_engineer.unit.orchestration.engine;

import com.prasad.agentic_software_engineer.orchestration.domain.EngineeringWorkflow;
import com.prasad.agentic_software_engineer.orchestration.domain.GateDefinition;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskStatus;
import com.prasad.agentic_software_engineer.orchestration.domain.TaskType;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowStatus;
import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowTask;
import com.prasad.agentic_software_engineer.orchestration.engine.TaskExecutionResult;
import com.prasad.agentic_software_engineer.orchestration.engine.WorkflowEngine;
import com.prasad.agentic_software_engineer.orchestration.engine.WorkflowGraphValidator;
import com.prasad.agentic_software_engineer.orchestration.engine.WorkflowTaskHandler;
import com.prasad.agentic_software_engineer.orchestration.engine.WorkflowTaskHandlerRegistry;
import com.prasad.agentic_software_engineer.orchestration.gate.WorkflowGateEvaluator;
import com.prasad.agentic_software_engineer.orchestration.repository.InMemoryWorkflowRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowEngineTest {

    private static final Instant NOW =
            Instant.parse("2026-08-21T12:00:00Z");

    private final Clock clock =
            Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void executesParallelBranchesBeforeJoin() {
        CountDownLatch parallelStart =
                new CountDownLatch(2);

        ConcurrentLinkedQueue<String> events =
                new ConcurrentLinkedQueue<>();

        WorkflowTaskHandler implementationHandler =
                parallelHandler(
                        TaskType.IMPLEMENTATION,
                        "implementation",
                        parallelStart,
                        events
                );

        WorkflowTaskHandler testingHandler =
                parallelHandler(
                        TaskType.TEST_GENERATION,
                        "tests",
                        parallelStart,
                        events
                );

        WorkflowTaskHandler validationHandler =
                new WorkflowTaskHandler() {

                    @Override
                    public TaskType supports() {
                        return TaskType.VALIDATION;
                    }

                    @Override
                    public TaskExecutionResult execute(
                            EngineeringWorkflow workflow,
                            WorkflowTask task
                    ) {
                        assertThat(
                                workflow.getContext()
                                        .contains("implementation")
                        ).isTrue();

                        assertThat(
                                workflow.getContext()
                                        .contains("tests")
                        ).isTrue();

                        events.add("validation");

                        return TaskExecutionResult.of(
                                "validation",
                                "passed"
                        );
                    }
                };

        try (WorkflowEngine engine = createEngine(
                List.of(
                        implementationHandler,
                        testingHandler,
                        validationHandler
                )
        )) {
            EngineeringWorkflow workflow =
                    new EngineeringWorkflow(
                            UUID.randomUUID(),
                            "Add redirect analytics",
                            NOW
                    );

            WorkflowTask implementation = task(
                    "Generate implementation",
                    TaskType.IMPLEMENTATION,
                    Set.of(),
                    GateDefinition.none()
            );

            WorkflowTask tests = task(
                    "Generate tests",
                    TaskType.TEST_GENERATION,
                    Set.of(),
                    GateDefinition.none()
            );

            WorkflowTask validation = task(
                    "Validate joined output",
                    TaskType.VALIDATION,
                    Set.of(
                            implementation.getId(),
                            tests.getId()
                    ),
                    GateDefinition.contextKeys(
                            "implementation",
                            "tests"
                    )
            );

            workflow.addTask(implementation);
            workflow.addTask(tests);
            workflow.addTask(validation);

            engine.execute(workflow);

            assertThat(workflow.getStatus())
                    .isEqualTo(WorkflowStatus.COMPLETED);

            assertThat(events)
                    .containsExactlyInAnyOrder(
                            "implementation",
                            "tests",
                            "validation"
                    );

            assertThat(events.peek())
                    .isNotEqualTo("validation");

            assertThat(validation.getStatus())
                    .isEqualTo(TaskStatus.SUCCEEDED);

            assertThat(
                    workflow.getContext()
                            .find("validation")
            ).isPresent();

            assertThat(
                    workflow.getContext()
                            .find("validation")
                            .orElseThrow()
                            .producingTaskId()
            ).isEqualTo(validation.getId());

            assertThat(
                    workflow.getContext()
                            .find("validation")
                            .orElseThrow()
                            .workflowRevision()
            ).isEqualTo(1);
        }
    }

    @Test
    void failsWorkflowWhenExitGateIsRejected() {
        WorkflowTaskHandler implementationHandler =
                new WorkflowTaskHandler() {

                    @Override
                    public TaskType supports() {
                        return TaskType.IMPLEMENTATION;
                    }

                    @Override
                    public TaskExecutionResult execute(
                            EngineeringWorkflow workflow,
                            WorkflowTask task
                    ) {
                        return TaskExecutionResult.empty();
                    }
                };

        try (WorkflowEngine engine = createEngine(
                List.of(implementationHandler)
        )) {
            EngineeringWorkflow workflow =
                    new EngineeringWorkflow(
                            UUID.randomUUID(),
                            "Generate an implementation",
                            NOW
                    );

            WorkflowTask implementation =
                    new WorkflowTask(
                            UUID.randomUUID(),
                            "Generate implementation",
                            TaskType.IMPLEMENTATION,
                            Set.of(),
                            GateDefinition.none(),
                            GateDefinition.contextKeys(
                                    "implementation.patch"
                            ),
                            1
                    );

            workflow.addTask(implementation);

            engine.execute(workflow);

            assertThat(workflow.getStatus())
                    .isEqualTo(WorkflowStatus.FAILED);

            assertThat(implementation.getStatus())
                    .isEqualTo(TaskStatus.FAILED);

            assertThat(implementation.getFailureMessage())
                    .contains("Exit gate rejected");
        }
    }

    private WorkflowEngine createEngine(
            List<WorkflowTaskHandler> handlers
    ) {
        return new WorkflowEngine(
                new WorkflowGraphValidator(),
                new WorkflowGateEvaluator(),
                new WorkflowTaskHandlerRegistry(handlers),
                new InMemoryWorkflowRepository(),
                clock
        );
    }

    private WorkflowTaskHandler parallelHandler(
            TaskType taskType,
            String contextKey,
            CountDownLatch parallelStart,
            ConcurrentLinkedQueue<String> events
    ) {
        return new WorkflowTaskHandler() {

            @Override
            public TaskType supports() {
                return taskType;
            }

            @Override
            public TaskExecutionResult execute(
                    EngineeringWorkflow workflow,
                    WorkflowTask task
            ) throws Exception {
                parallelStart.countDown();

                boolean bothBranchesStarted =
                        parallelStart.await(
                                2,
                                TimeUnit.SECONDS
                        );

                if (!bothBranchesStarted) {
                    throw new IllegalStateException(
                            "Parallel branches did not start together"
                    );
                }

                events.add(contextKey);

                return TaskExecutionResult.of(
                        contextKey,
                        "completed"
                );
            }
        };
    }

    private WorkflowTask task(
            String name,
            TaskType taskType,
            Set<UUID> dependencies,
            GateDefinition exitGate
    ) {
        return new WorkflowTask(
                UUID.randomUUID(),
                name,
                taskType,
                dependencies,
                GateDefinition.dependenciesSucceeded(),
                exitGate,
                1
        );
    }
}