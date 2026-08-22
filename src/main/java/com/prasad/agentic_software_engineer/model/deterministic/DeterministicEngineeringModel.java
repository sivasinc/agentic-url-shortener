package com.prasad.agentic_software_engineer.model.deterministic;

import com.prasad.agentic_software_engineer.model.DocumentationProposal;
import com.prasad.agentic_software_engineer.model.EngineeringModel;
import com.prasad.agentic_software_engineer.model.EngineeringPlan;
import com.prasad.agentic_software_engineer.model.EngineeringTaskPlan;
import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.RepositoryContext;
import com.prasad.agentic_software_engineer.model.RequirementAnalysis;
import com.prasad.agentic_software_engineer.model.RequirementContext;
import com.prasad.agentic_software_engineer.model.ValidationFailure;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@ConditionalOnProperty(
        name = "agentic.model.provider",
        havingValue = "deterministic",
        matchIfMissing = true
)
public class DeterministicEngineeringModel
        implements EngineeringModel {

    private final DeterministicAnalyticsPatchFactory
            analyticsPatchFactory;
    private final DeterministicGreenfieldPatchFactory
            greenfieldPatchFactory;

    public DeterministicEngineeringModel(
            DeterministicAnalyticsPatchFactory analyticsPatchFactory,
            DeterministicGreenfieldPatchFactory greenfieldPatchFactory
    ) {
        this.analyticsPatchFactory = analyticsPatchFactory;
        this.greenfieldPatchFactory = greenfieldPatchFactory;
    }

    @Override
    public RequirementAnalysis analyzeRequirement(
            RequirementContext context
    ) {
        String requirement =
                context.rawRequirement().trim();

        boolean ambiguous =
                context.clarificationHistory().isEmpty() &&
                        (context.scenarioType() ==
                                com.prasad.agentic_software_engineer.model.ScenarioType.AMBIGUOUS ||
                                isAmbiguous(requirement));

        List<String> ambiguities = ambiguous
                ? List.of(
                "Which measurable behavior must change?",
                "What acceptance criteria define success?"
        )
                : List.of();

        List<String> criteria = ambiguous
                ? List.of()
                : List.of(
                "The requested behavior is implemented",
                "Existing behavior remains compatible",
                "Unit and integration tests pass",
                "The final source diff is reviewable"
        );

        String normalizedRequirement =
                context.clarificationHistory().isEmpty()
                        ? requirement
                        : requirement + " Clarifications: " +
                        String.join(
                                "; ",
                                context.clarificationHistory()
                        );

        return new RequirementAnalysis(
                normalizedRequirement,
                criteria,
                ambiguities,
                List.of(
                        "The repository uses its existing build conventions"
                ),
                List.of(
                        "Generated changes may introduce regressions"
                ),
                ambiguous
        );
    }

    @Override
    public EngineeringPlan createPlan(
            RepositoryContext repository
    ) {
        boolean greenfield = repository.scenarioType() ==
                com.prasad.agentic_software_engineer.model.ScenarioType.GREENFIELD;

        return new EngineeringPlan(
                greenfield
                        ? "Inspect the build-only seed, establish the application " +
                        "boundary, generate production and test code, validate the " +
                        "new application, and document the outcome."
                        : "Inspect the existing repository, preserve its behavior, " +
                        "implement the requested change, generate regression tests, " +
                        "validate the modified workspace, and document the outcome.",
                List.of(
                        task(
                                "repository",
                                "Repository analysis",
                                greenfield
                                        ? "Identify build constraints and confirm that application source is absent"
                                        : "Identify impacted modules, APIs, schemas and tests",
                                List.of(),
                                false
                        ),
                        task(
                                "implementation",
                                "Implementation",
                                greenfield
                                        ? "Generate the new application boundary and production source"
                                        : "Generate production source changes compatible with the repository",
                                List.of("repository"),
                                true
                        ),
                        task(
                                "tests",
                                "Test generation",
                                "Generate acceptance-criteria tests",
                                List.of("repository"),
                                true
                        ),
                        task(
                                "validation",
                                "Validation",
                                "Run the repository build against modified code",
                                List.of(
                                        "implementation",
                                        "tests"
                                ),
                                false
                        ),
                        task(
                                "documentation",
                                "Documentation",
                                "Document the validated engineering outcome",
                                List.of("validation"),
                                false
                        )
                ),
                List.of(
                        greenfield
                                ? "A minimal seed provides fewer conventions for the model to infer"
                                : "Generated code may not match repository conventions"
                ),
                List.of(
                        greenfield
                                ? "An in-memory implementation favors a fully validated vertical slice over persistence"
                                : "A bounded implementation favors reviewability over broad changes"
                )
        );
    }

    @Override
    public PatchProposal generateImplementation(
            EngineeringPlan plan,
            RepositoryContext repository
    ) {
        if (repository.scenarioType() ==
                com.prasad.agentic_software_engineer.model.ScenarioType.GREENFIELD) {
            return greenfieldPatchFactory.implementation();
        }

        if (repository.requirement()
                .normalizedRequirement()
                .toLowerCase(Locale.ROOT)
                .contains("demonstrate repair")) {
            return analyticsPatchFactory
                    .intentionallyFailingImplementation();
        }

        return analyticsPatchFactory
                .implementation();
    }

    @Override
    public PatchProposal generateTests(
            EngineeringPlan plan,
            PatchProposal implementation,
            RepositoryContext repository
    ) {
        if (repository.scenarioType() ==
                com.prasad.agentic_software_engineer.model.ScenarioType.GREENFIELD) {
            return greenfieldPatchFactory.tests();
        }

        return analyticsPatchFactory.tests();
    }

    @Override
    public PatchProposal repair(
            EngineeringPlan plan,
            PatchProposal previousPatch,
            ValidationFailure failure,
            RepositoryContext repository
    ) {
        if (repository.scenarioType() ==
                com.prasad.agentic_software_engineer.model.ScenarioType.GREENFIELD) {
            return greenfieldPatchFactory.completeChange();
        }

        return analyticsPatchFactory
                .completeRepair();
    }

    @Override
    public DocumentationProposal generateDocumentation(
            EngineeringPlan plan,
            PatchProposal validatedPatch,
            RepositoryContext repository
    ) {
        return new DocumentationProposal(
                "The agent generated and validated a bounded software change.",
                "The change follows the repository build and test conventions.",
                List.of(
                        "The deterministic provider supports controlled fixture scenarios"
                )
        );
    }

    private EngineeringTaskPlan task(
            String id,
            String name,
            String description,
            List<String> dependencies,
            boolean parallelizable
    ) {
        return new EngineeringTaskPlan(
                id,
                name,
                description,
                dependencies,
                parallelizable,
                false
        );
    }

    private boolean isAmbiguous(String requirement) {
        String normalized =
                requirement.toLowerCase(Locale.ROOT);

        boolean vagueVerb =
                normalized.contains("improve") ||
                        normalized.contains("better") ||
                        normalized.contains("enhance");

        boolean measurableDetail =
                normalized.contains("total") ||
                        normalized.contains("daily") ||
                        normalized.contains("endpoint") ||
                        normalized.contains("status") ||
                        normalized.contains("count");

        return vagueVerb && !measurableDetail;
    }
}
