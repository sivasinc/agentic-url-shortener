package com.prasad.agentic_software_engineer.unit.validation;

import com.prasad.agentic_software_engineer.config.AgentExecutionProperties;
import com.prasad.agentic_software_engineer.validation.BuildValidationResult;
import com.prasad.agentic_software_engineer.validation.MavenBuildTool;
import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MavenBuildToolTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void capturesSuccessfulValidationEvidence()
            throws IOException {
        EngineeringWorkspace workspace =
                workspaceWithWrapper(0);

        MavenBuildTool tool =
                new MavenBuildTool(properties());

        BuildValidationResult result =
                tool.validate(workspace, 1);

        assertThat(result.successful()).isTrue();
        assertThat(result.exitCode()).isZero();
        assertThat(result.timedOut()).isFalse();
        assertThat(
                workspace.root()
                        .resolve(result.logArtifact())
        ).isRegularFile();
    }

    @Test
    void capturesFailedValidationEvidence()
            throws IOException {
        EngineeringWorkspace workspace =
                workspaceWithWrapper(7);

        MavenBuildTool tool =
                new MavenBuildTool(properties());

        BuildValidationResult result =
                tool.validate(workspace, 1);

        assertThat(result.successful()).isFalse();
        assertThat(result.exitCode()).isEqualTo(7);
        assertThat(result.toFailure().summary())
                .contains("code 7");
    }

    private EngineeringWorkspace workspaceWithWrapper(
            int exitCode
    ) throws IOException {
        Path root =
                temporaryDirectory.resolve(
                        UUID.randomUUID().toString()
                );

        Path repository =
                root.resolve("repository");
        Path baseline =
                root.resolve("snapshots/baseline");
        Path artifacts =
                root.resolve("artifacts");
        Path logs =
                root.resolve("logs");

        Files.createDirectories(repository);
        Files.createDirectories(baseline);
        Files.createDirectories(artifacts);
        Files.createDirectories(logs);

        if (isWindows()) {
            Files.writeString(
                    repository.resolve("mvnw.cmd"),
                    """
                    @echo off
                    echo fixture validation
                    exit /b %d
                    """.formatted(exitCode)
            );
        } else {
            Path wrapper =
                    repository.resolve("mvnw");

            Files.writeString(
                    wrapper,
                    """
                    #!/usr/bin/env sh
                    echo fixture validation
                    exit %d
                    """.formatted(exitCode)
            );

            Files.setPosixFilePermissions(
                    wrapper,
                    Set.of(
                            PosixFilePermission.OWNER_READ,
                            PosixFilePermission.OWNER_WRITE,
                            PosixFilePermission.OWNER_EXECUTE
                    )
            );
        }

        return new EngineeringWorkspace(
                UUID.randomUUID(),
                1,
                root,
                repository,
                baseline,
                artifacts,
                logs,
                Map.of()
        );
    }

    private AgentExecutionProperties properties() {
        return new AgentExecutionProperties(
                2,
                Duration.ofSeconds(10),
                10_000,
                30,
                524_288
        );
    }

    private boolean isWindows() {
        return System.getProperty("os.name")
                .toLowerCase()
                .contains("win");
    }
}