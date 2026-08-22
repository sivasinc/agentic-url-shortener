package com.prasad.agentic_software_engineer.unit.documentation;

import com.prasad.agentic_software_engineer.agent.documentation.DocumentationAgent;
import com.prasad.agentic_software_engineer.documentation.EngineeringOutcomeArtifact;
import com.prasad.agentic_software_engineer.documentation.EngineeringOutcomeService;
import com.prasad.agentic_software_engineer.model.DocumentationProposal;
import com.prasad.agentic_software_engineer.model.EngineeringPlan;
import com.prasad.agentic_software_engineer.model.EngineeringTaskPlan;
import com.prasad.agentic_software_engineer.model.FileChangeType;
import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.ProposedFileChange;
import com.prasad.agentic_software_engineer.model.RepositoryContext;
import com.prasad.agentic_software_engineer.model.RequirementAnalysis;
import com.prasad.agentic_software_engineer.model.ScenarioType;
import com.prasad.agentic_software_engineer.patch.AppliedPatch;
import com.prasad.agentic_software_engineer.validation.BuildValidationResult;
import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EngineeringOutcomeServiceTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void writesConsolidatedReviewerDeliverable() throws Exception {
        DocumentationAgent agent = mock(DocumentationAgent.class);
        when(agent.generate(any(), any(), any())).thenReturn(
                new DocumentationProposal(
                        "Document the generated analytics endpoint.",
                        "Analytics is isolated behind service and API layers.",
                        List.of("Workflow execution state remains in memory")
                )
        );

        EngineeringOutcomeService service =
                new EngineeringOutcomeService(agent);
        EngineeringWorkspace workspace = workspace();
        RepositoryContext repository = mock(RepositoryContext.class);
        when(repository.scenarioType()).thenReturn(ScenarioType.BROWNFIELD);

        EngineeringOutcomeArtifact result = service.generate(
                2,
                requirement(),
                plan(),
                repository,
                patch(),
                appliedPatch(),
                validation(),
                workspace
        );

        assertThat(result.relativePath())
                .isEqualTo("artifacts/engineering-outcome.md");
        assertThat(result.content())
                .contains(
                        "## Plan and rationale",
                        "## Scenario",
                        "## Generated artifacts",
                        "## Validation evidence",
                        "## Assumptions",
                        "## Risks and trade-offs",
                        "## Limitations"
                );
        assertThat(workspace.root().resolve(result.relativePath()))
                .hasContent(result.content());
    }

    private RequirementAnalysis requirement() {
        return new RequirementAnalysis(
                "Add redirect analytics",
                List.of("Maven tests pass"),
                List.of(),
                List.of("Use UTC"),
                List.of("Aggregation may become expensive"),
                false
        );
    }

    private EngineeringPlan plan() {
        return new EngineeringPlan(
                "Implement a bounded analytics slice.",
                List.of(new EngineeringTaskPlan(
                        "implementation",
                        "Implementation",
                        "Add analytics source changes",
                        List.of(),
                        false,
                        false
                )),
                List.of("Generated code may regress behavior"),
                List.of("Prefer reviewability over broad scope")
        );
    }

    private PatchProposal patch() {
        return new PatchProposal(
                "Add analytics",
                List.of(new ProposedFileChange(
                        FileChangeType.CREATE,
                        "src/main/java/example/Analytics.java",
                        null,
                        "class Analytics {}",
                        "Keeps analytics behavior isolated"
                )),
                List.of("Existing build conventions remain valid"),
                List.of("New endpoint adds maintenance cost")
        );
    }

    private AppliedPatch appliedPatch() {
        return new AppliedPatch(
                List.of("src/main/java/example/Analytics.java"),
                "diff --agentic",
                Map.of("src/main/java/example/Analytics.java", "abc123"),
                "artifacts/applied.patch"
        );
    }

    private BuildValidationResult validation() {
        return new BuildValidationResult(
                true,
                "./mvnw clean test",
                0,
                false,
                Duration.ofSeconds(3),
                "BUILD SUCCESS",
                "logs/maven-test-attempt-1.log"
        );
    }

    private EngineeringWorkspace workspace() throws Exception {
        Path repository = Files.createDirectories(
                temporaryDirectory.resolve("repository")
        );
        Path baseline = Files.createDirectories(
                temporaryDirectory.resolve("snapshots/baseline")
        );
        Path artifacts = Files.createDirectories(
                temporaryDirectory.resolve("artifacts")
        );
        Path logs = Files.createDirectories(
                temporaryDirectory.resolve("logs")
        );
        return new EngineeringWorkspace(
                UUID.randomUUID(),
                2,
                temporaryDirectory,
                repository,
                baseline,
                artifacts,
                logs,
                Map.of()
        );
    }
}
