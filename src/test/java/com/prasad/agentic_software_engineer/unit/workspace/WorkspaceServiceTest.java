package com.prasad.agentic_software_engineer.unit.workspace;

import com.prasad.agentic_software_engineer.config.AgentRepositoryProperties;
import com.prasad.agentic_software_engineer.config.AgentWorkspaceProperties;
import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import com.prasad.agentic_software_engineer.workspace.FileHashService;
import com.prasad.agentic_software_engineer.workspace.SafePathResolver;
import com.prasad.agentic_software_engineer.workspace.WorkspaceException;
import com.prasad.agentic_software_engineer.workspace.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkspaceServiceTest {

    @TempDir
    Path temporaryDirectory;

    private Path allowedRoot;
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() throws Exception {
        allowedRoot =
                temporaryDirectory.resolve(
                        "scenario-repositories"
                );

        Files.createDirectories(allowedRoot);

        workspaceService = new WorkspaceService(
                new AgentWorkspaceProperties(
                        temporaryDirectory.resolve(
                                "agent-workspaces"
                        )
                ),
                new AgentRepositoryProperties(
                        allowedRoot,
                        100,
                        1_000_000,
                        100_000
                ),
                new SafePathResolver(),
                new FileHashService()
        );
    }

    @Test
    void createsIsolatedCopyAndRollsBackChanges()
            throws Exception {
        Path source =
                createSourceRepository("brownfield");

        EngineeringWorkspace workspace =
                workspaceService.create(
                        UUID.randomUUID(),
                        1,
                        Path.of("brownfield")
                );

        Path originalFile =
                source.resolve(
                        "src/main/java/example/App.java"
                );

        Path workspaceFile =
                workspace.repository().resolve(
                        "src/main/java/example/App.java"
                );

        assertThat(workspaceFile).exists();

        Files.writeString(
                workspaceFile,
                "class Changed {}"
        );

        assertThat(Files.readString(originalFile))
                .isEqualTo("class App {}");

        assertThat(
                workspaceService.isClean(workspace)
        ).isFalse();

        workspaceService.rollback(workspace);

        assertThat(
                Files.readString(workspaceFile)
        ).isEqualTo("class App {}");

        assertThat(
                workspaceService.isClean(workspace)
        ).isTrue();
    }

    @Test
    void rejectsSourceOutsideApprovedRoot() {
        assertThatThrownBy(
                () -> workspaceService.create(
                        UUID.randomUUID(),
                        1,
                        Path.of("../outside")
                )
        )
                .isInstanceOf(WorkspaceException.class)
                .hasMessageContaining("escapes");
    }

    @Test
    void refusesToOverwriteExistingWorkspace()
            throws Exception {
        createSourceRepository("brownfield");

        UUID workflowId = UUID.randomUUID();

        workspaceService.create(
                workflowId,
                1,
                Path.of("brownfield")
        );

        assertThatThrownBy(
                () -> workspaceService.create(
                        workflowId,
                        1,
                        Path.of("brownfield")
                )
        )
                .isInstanceOf(WorkspaceException.class)
                .hasMessageContaining("already exists");
    }

    private Path createSourceRepository(String name)
            throws Exception {
        Path source = allowedRoot.resolve(name);

        Path javaSource = source.resolve(
                "src/main/java/example/App.java"
        );

        Files.createDirectories(
                javaSource.getParent()
        );

        Files.writeString(
                source.resolve("pom.xml"),
                "<project/>"
        );

        Files.writeString(
                javaSource,
                "class App {}"
        );

        return source;
    }
}