package com.prasad.agentic_software_engineer.unit.model;

import com.prasad.agentic_software_engineer.agent.repository.RepositoryAssessment;
import com.prasad.agentic_software_engineer.model.EngineeringPlan;
import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.RepositoryContext;
import com.prasad.agentic_software_engineer.model.RequirementAnalysis;
import com.prasad.agentic_software_engineer.model.RequirementContext;
import com.prasad.agentic_software_engineer.model.ScenarioType;
import com.prasad.agentic_software_engineer.model.deterministic.DeterministicEngineeringModel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicEngineeringModelTest {

    private final DeterministicEngineeringModel model =
            new DeterministicEngineeringModel(
                    new com.prasad.agentic_software_engineer
                            .model.deterministic
                            .DeterministicAnalyticsPatchFactory()
            );

    @Test
    void identifiesAmbiguousRequirement() {
        RequirementAnalysis analysis =
                model.analyzeRequirement(
                        new RequirementContext(
                                ScenarioType.AMBIGUOUS,
                                "Improve URL analytics",
                                List.of()
                        )
                );

        assertThat(analysis.requiresClarification())
                .isTrue();

        assertThat(analysis.ambiguities())
                .isNotEmpty();
    }

    @Test
    void normalizesWellDefinedRequirement() {
        RequirementAnalysis analysis =
                model.analyzeRequirement(
                        new RequirementContext(
                                ScenarioType.BROWNFIELD,
                                "Add total and daily redirect counts",
                                List.of()
                        )
                );

        assertThat(analysis.requiresClarification())
                .isFalse();

        assertThat(analysis.acceptanceCriteria())
                .isNotEmpty();
    }

    @Test
    void clarificationResolvesAmbiguousRequirement() {
        RequirementAnalysis analysis = model.analyzeRequirement(
                new RequirementContext(
                        ScenarioType.AMBIGUOUS,
                        "Improve URL analytics",
                        List.of(
                                "Return total and UTC daily redirect counts"
                        )
                )
        );

        assertThat(analysis.requiresClarification()).isFalse();
        assertThat(analysis.normalizedRequirement())
                .contains("UTC daily redirect counts");
    }

    @Test
    void generatesPlanSourceAndTestChanges() {
        RequirementAnalysis requirement =
                model.analyzeRequirement(
                        new RequirementContext(
                                ScenarioType.BROWNFIELD,
                                "Add total redirect count endpoint",
                                List.of()
                        )
                );

        RepositoryContext repository =
                new RepositoryContext(
                        requirement,
                        assessment(),
                        Map.of()
                );

        EngineeringPlan plan =
                model.createPlan(repository);

        PatchProposal implementation =
                model.generateImplementation(
                        plan,
                        repository
                );

        PatchProposal tests =
                model.generateTests(
                        plan,
                        implementation,
                        repository
                );

        assertThat(plan.tasks())
                .extracting("id")
                .contains(
                        "implementation",
                        "tests",
                        "validation"
                );

        assertThat(implementation.changes())
                .anyMatch(
                        change ->
                                change.path()
                                        .startsWith(
                                                "src/main/java/"
                                        )
                );

        assertThat(tests.changes())
                .anyMatch(
                        change ->
                                change.path()
                                        .startsWith(
                                                "src/test/java/"
                                        )
                );
    }

    @Test
    void createsControlledFailureForRepairDemonstration() {
        RequirementAnalysis requirement = model.analyzeRequirement(
                new RequirementContext(
                        ScenarioType.BROWNFIELD,
                        "Add redirect analytics and demonstrate repair",
                        List.of()
                )
        );
        RepositoryContext repository = new RepositoryContext(
                requirement,
                assessment(),
                Map.of()
        );

        PatchProposal proposal = model.generateImplementation(
                model.createPlan(repository),
                repository
        );

        assertThat(proposal.changes())
                .anyMatch(change ->
                        change.content().contains(
                                "unresolvedRepairDemoSymbol"
                        )
                );
    }

    private RepositoryAssessment assessment() {
        return new RepositoryAssessment(
                1,
                Set.of("MAVEN"),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
