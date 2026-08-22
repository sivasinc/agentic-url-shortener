package com.prasad.agentic_software_engineer.unit.agent.repository;

import com.prasad.agentic_software_engineer.agent.repository.RepositoryAnalysisAgent;
import com.prasad.agentic_software_engineer.agent.repository.RepositoryAssessment;
import com.prasad.agentic_software_engineer.config.AgentRepositoryProperties;
import com.prasad.agentic_software_engineer.tool.repository.ControlledRepositoryTools;
import com.prasad.agentic_software_engineer.workspace.SafePathResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryAnalysisAgentTest {

    @TempDir
    Path repository;

    private RepositoryAnalysisAgent agent;

    @BeforeEach
    void setUp() {
        ControlledRepositoryTools tools =
                new ControlledRepositoryTools(
                        new SafePathResolver(),
                        new AgentRepositoryProperties(
                                repository,
                                100,
                                1_000_000,
                                100_000
                        )
                );

        agent = new RepositoryAnalysisAgent(tools);
    }

    @Test
    void discoversArchitectureAndImpactedFiles()
            throws Exception {
        write(
                "pom.xml",
                "<project/>"
        );

        write(
                "src/main/java/example/RedirectController.java",
                """
                class RedirectController {
                    void redirect() {
                    }
                }
                """
        );

        write(
                "src/test/java/example/RedirectControllerTest.java",
                """
                class RedirectControllerTest {
                }
                """
        );

        write(
                "src/main/resources/db/migration/V1__urls.sql",
                "CREATE TABLE short_urls (id BIGINT);"
        );

        write(
                "src/main/resources/application.yaml",
                "spring.application.name: sample"
        );

        write(
                "README.md",
                "# Sample"
        );

        RepositoryAssessment assessment =
                agent.analyze(
                        repository,
                        "Add redirect analytics"
                );

        assertThat(assessment.buildSystems())
                .contains("MAVEN");

        assertThat(assessment.sourceFiles())
                .contains(
                        "src/main/java/example/RedirectController.java"
                );

        assertThat(assessment.testFiles())
                .contains(
                        "src/test/java/example/RedirectControllerTest.java"
                );

        assertThat(assessment.migrations())
                .contains(
                        "src/main/resources/db/migration/V1__urls.sql"
                );

        assertThat(assessment.configurationFiles())
                .contains(
                        "src/main/resources/application.yaml"
                );

        assertThat(assessment.documentationFiles())
                .contains("README.md");

        assertThat(assessment.impactedFiles())
                .contains(
                        "src/main/java/example/RedirectController.java",
                        "src/test/java/example/RedirectControllerTest.java"
                );
    }

    private void write(
            String relativePath,
            String content
    ) throws Exception {
        Path file = repository.resolve(relativePath);

        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }
}