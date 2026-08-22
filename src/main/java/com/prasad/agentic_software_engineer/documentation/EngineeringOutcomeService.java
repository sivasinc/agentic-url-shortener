package com.prasad.agentic_software_engineer.documentation;

import com.prasad.agentic_software_engineer.agent.documentation.DocumentationAgent;
import com.prasad.agentic_software_engineer.model.DocumentationProposal;
import com.prasad.agentic_software_engineer.model.EngineeringPlan;
import com.prasad.agentic_software_engineer.model.EngineeringTaskPlan;
import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.ProposedFileChange;
import com.prasad.agentic_software_engineer.model.RepositoryContext;
import com.prasad.agentic_software_engineer.model.RequirementAnalysis;
import com.prasad.agentic_software_engineer.patch.AppliedPatch;
import com.prasad.agentic_software_engineer.validation.BuildValidationResult;
import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EngineeringOutcomeService {

    private static final String FILE_NAME = "engineering-outcome.md";

    private final DocumentationAgent documentationAgent;

    public EngineeringOutcomeArtifact generate(
            long revision,
            RequirementAnalysis requirement,
            EngineeringPlan plan,
            RepositoryContext repository,
            PatchProposal validatedPatch,
            AppliedPatch appliedPatch,
            BuildValidationResult validation,
            EngineeringWorkspace workspace
    ) {
        DocumentationProposal documentation = documentationAgent.generate(
                plan,
                validatedPatch,
                repository
        );

        String content = render(
                revision,
                requirement,
                plan,
                repository,
                validatedPatch,
                appliedPatch,
                validation,
                documentation
        );

        Path file = workspace.artifacts().resolve(FILE_NAME).normalize();
        Path artifactRoot = workspace.artifacts().toAbsolutePath().normalize();
        if (!file.toAbsolutePath().normalize().startsWith(artifactRoot)) {
            throw new IllegalStateException(
                    "Engineering outcome escaped the artifact directory"
            );
        }

        try {
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to write consolidated engineering outcome",
                    exception
            );
        }

        return new EngineeringOutcomeArtifact(
                workspace.root().relativize(file)
                        .toString()
                        .replace('\\', '/'),
                content
        );
    }

    private String render(
            long revision,
            RequirementAnalysis requirement,
            EngineeringPlan plan,
            RepositoryContext repository,
            PatchProposal patch,
            AppliedPatch appliedPatch,
            BuildValidationResult validation,
            DocumentationProposal documentation
    ) {
        StringBuilder markdown = new StringBuilder();
        markdown.append("# Consolidated Engineering Outcome\n\n")
                .append("## Scenario\n\n")
                .append(repository.scenarioType())
                .append("\n\n")
                .append("## Requirement\n\n")
                .append(requirement.normalizedRequirement())
                .append("\n\nWorkflow revision: ")
                .append(revision)
                .append("\n\n## Plan and rationale\n\n")
                .append(plan.rationale())
                .append("\n\n");

        for (EngineeringTaskPlan task : plan.tasks()) {
            markdown.append("- **")
                    .append(task.name())
                    .append(":** ")
                    .append(task.description())
                    .append("; dependencies=")
                    .append(task.dependencyIds().isEmpty()
                            ? "none"
                            : String.join(", ", task.dependencyIds()))
                    .append("\n");
        }

        markdown.append("\n## Implementation rationale\n\n");
        for (ProposedFileChange change : patch.changes()) {
            markdown.append("- `")
                    .append(change.path())
                    .append("` (")
                    .append(change.type())
                    .append("): ")
                    .append(change.rationale())
                    .append("\n");
        }

        markdown.append("\n## Generated artifacts\n\n")
                .append("- Changed files: ")
                .append(String.join(", ", appliedPatch.changedFiles()))
                .append("\n- Diff: `")
                .append(appliedPatch.diffArtifact())
                .append("`\n- Validation log: `")
                .append(validation.logArtifact())
                .append("`\n- Audit evidence: `artifacts/audit/`\n")
                .append("- Resulting SHA-256 hashes are recorded in the applied-patch evidence.\n")
                .append("\n## Validation evidence\n\n")
                .append("- Command: `")
                .append(validation.command())
                .append("`\n- Successful: ")
                .append(validation.successful())
                .append("\n- Exit code: ")
                .append(validation.exitCode())
                .append("\n- Timed out: ")
                .append(validation.timedOut())
                .append("\n- Duration: ")
                .append(validation.duration())
                .append("\n\n## Architecture outcome\n\n")
                .append(documentation.architectureSummary())
                .append("\n\n## Documentation outcome\n\n")
                .append(documentation.readmeSection())
                .append("\n\n## Assumptions\n\n");

        appendList(markdown, distinct(
                requirement.assumptions(),
                patch.assumptions()
        ));

        markdown.append("\n## Risks and trade-offs\n\n");
        appendList(markdown, distinct(
                requirement.risks(),
                plan.risks(),
                plan.tradeOffs(),
                patch.risks()
        ));

        markdown.append("\n## Limitations\n\n");
        appendList(markdown, distinct(
                documentation.limitations(),
                List.of(
                        "Active workflow state is in memory and cannot resume after restart.",
                        "The controlled build tool currently supports Maven repositories.",
                        "Live OpenAI execution requires reviewer-supplied credentials and network access.",
                        "Generated changes remain in the isolated workspace and are not automatically published."
                )
        ));

        markdown.append("\n## Release decision\n\n")
                .append("Validation passed. The change is awaiting explicit ")
                .append("human release-readiness approval.\n");

        return markdown.toString();
    }

    @SafeVarargs
    private final List<String> distinct(List<String>... values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (List<String> items : values) {
            result.addAll(items);
        }
        return List.copyOf(result);
    }

    private void appendList(StringBuilder markdown, List<String> values) {
        if (values.isEmpty()) {
            markdown.append("- None identified.\n");
            return;
        }
        values.forEach(value -> markdown.append("- ")
                .append(value)
                .append("\n"));
    }
}
