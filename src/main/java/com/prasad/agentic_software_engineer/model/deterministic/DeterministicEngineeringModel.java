package com.prasad.agentic_software_engineer.model.deterministic;

import com.prasad.agentic_software_engineer.model.DocumentationProposal;
import com.prasad.agentic_software_engineer.model.EngineeringModel;
import com.prasad.agentic_software_engineer.model.EngineeringPlan;
import com.prasad.agentic_software_engineer.model.EngineeringTaskPlan;
import com.prasad.agentic_software_engineer.model.FileChangeType;
import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.ProposedFileChange;
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

    @Override
    public RequirementAnalysis analyzeRequirement(
            RequirementContext context
    ) {
        String requirement =
                context.rawRequirement().trim();

        boolean ambiguous =
                isAmbiguous(requirement);

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

        return new RequirementAnalysis(
                requirement,
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
        return new EngineeringPlan(
                "Inspect the repository, implement the requirement, " +
                        "generate tests, validate the modified workspace, " +
                        "and document the final outcome.",
                List.of(
                        task(
                                "repository",
                                "Repository analysis",
                                "Identify impacted modules, APIs, schemas and tests",
                                List.of(),
                                false
                        ),
                        task(
                                "implementation",
                                "Implementation",
                                "Generate production source changes",
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
                        "Generated code may not match repository conventions"
                ),
                List.of(
                        "A bounded implementation favors reviewability over broad changes"
                )
        );
    }

    @Override
    public PatchProposal generateImplementation(
            EngineeringPlan plan,
            RepositoryContext repository
    ) {
        return new PatchProposal(
                "Generate a concrete production source change",
                List.of(
                        new ProposedFileChange(
                                FileChangeType.CREATE,
                                "src/main/java/generated/AgentGeneratedChange.java",
                                null,
                                """
                                package generated;

                                public final class AgentGeneratedChange {

                                    private AgentGeneratedChange() {
                                    }

                                    public static String status() {
                                        return "implemented";
                                    }
                                }
                                """,
                                "Creates a real compilable source artifact"
                        )
                ),
                List.of(
                        "The fixture repository compiles Java sources under src/main/java"
                ),
                List.of(
                        "The generated package is intentionally isolated"
                )
        );
    }

    @Override
    public PatchProposal generateTests(
            EngineeringPlan plan,
            PatchProposal implementation,
            RepositoryContext repository
    ) {
        return new PatchProposal(
                "Generate a test for the production change",
                List.of(
                        new ProposedFileChange(
                                FileChangeType.CREATE,
                                "src/test/java/generated/AgentGeneratedChangeTest.java",
                                null,
                                """
                                package generated;

                                import org.junit.jupiter.api.Test;

                                import static org.assertj.core.api.Assertions.assertThat;

                                class AgentGeneratedChangeTest {

                                    @Test
                                    void reportsImplementedStatus() {
                                        assertThat(
                                                AgentGeneratedChange.status()
                                        ).isEqualTo("implemented");
                                    }
                                }
                                """,
                                "Validates the generated production behavior"
                        )
                ),
                List.of(
                        "The fixture uses JUnit and AssertJ"
                ),
                List.of(
                        "Test dependencies must already be available"
                )
        );
    }

    @Override
    public PatchProposal repair(
            EngineeringPlan plan,
            PatchProposal previousPatch,
            ValidationFailure failure,
            RepositoryContext repository
    ) {
        return new PatchProposal(
                "Repair the generated implementation and tests",
                List.of(
                        new ProposedFileChange(
                                FileChangeType.CREATE,
                                "src/main/java/generated/AgentGeneratedChange.java",
                                null,
                                """
                                package generated;
    
                                public final class AgentGeneratedChange {
    
                                    private AgentGeneratedChange() {
                                    }
    
                                    public static String status() {
                                        return "implemented";
                                    }
                                }
                                """,
                                "Creates corrected compilable production code"
                        ),
                        new ProposedFileChange(
                                FileChangeType.CREATE,
                                "src/test/java/generated/AgentGeneratedChangeTest.java",
                                null,
                                """
                                package generated;
    
                                import org.junit.jupiter.api.Test;
    
                                import static org.assertj.core.api.Assertions.assertThat;
    
                                class AgentGeneratedChangeTest {
    
                                    @Test
                                    void reportsImplementedStatus() {
                                        assertThat(
                                                AgentGeneratedChange.status()
                                        ).isEqualTo("implemented");
                                    }
                                }
                                """,
                                "Restores the complete test operation after rollback"
                        )
                ),
                List.of(
                        "The fixture repository uses Maven, JUnit and AssertJ"
                ),
                List.of(
                        "A non-fixture failure may require model or human review"
                )
        );
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