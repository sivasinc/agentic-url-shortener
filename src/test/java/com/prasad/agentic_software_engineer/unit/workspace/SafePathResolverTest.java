package com.prasad.agentic_software_engineer.unit.workspace;

import com.prasad.agentic_software_engineer.workspace.SafePathResolver;
import com.prasad.agentic_software_engineer.workspace.WorkspaceException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafePathResolverTest {

    @TempDir
    Path temporaryDirectory;

    private final SafePathResolver resolver =
            new SafePathResolver();

    @Test
    void resolvesPathInsideApprovedRoot() {
        Path approvedRoot = temporaryDirectory
                .toAbsolutePath()
                .normalize();

        Path resolved = resolver.resolve(
                temporaryDirectory,
                "repository/src/App.java"
        );

        assertThat(
                resolved.startsWith(approvedRoot)
        ).isTrue();

        assertThat(resolved)
                .isEqualTo(
                        approvedRoot.resolve(
                                "repository/src/App.java"
                        ).normalize()
                );
    }

    @Test
    void rejectsPathTraversal() {
        assertThatThrownBy(
                () -> resolver.resolve(
                        temporaryDirectory,
                        "../secret.txt"
                )
        )
                .isInstanceOf(WorkspaceException.class)
                .hasMessageContaining("escapes");
    }

    @Test
    void rejectsAbsolutePath() {
        Path absolute =
                temporaryDirectory
                        .resolve("secret.txt")
                        .toAbsolutePath();

        assertThatThrownBy(
                () -> resolver.resolve(
                        temporaryDirectory,
                        absolute
                )
        )
                .isInstanceOf(WorkspaceException.class)
                .hasMessageContaining("Absolute");
    }
}