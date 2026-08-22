package com.prasad.agentic_software_engineer.unit.patch;

import com.prasad.agentic_software_engineer.config.AgentExecutionProperties;
import com.prasad.agentic_software_engineer.config.AgentRepositoryProperties;
import com.prasad.agentic_software_engineer.config.AgentWorkspaceProperties;
import com.prasad.agentic_software_engineer.model.FileChangeType;
import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.ProposedFileChange;
import com.prasad.agentic_software_engineer.patch.AppliedPatch;
import com.prasad.agentic_software_engineer.patch.DiffService;
import com.prasad.agentic_software_engineer.patch.PatchPolicy;
import com.prasad.agentic_software_engineer.patch.PatchService;
import com.prasad.agentic_software_engineer.patch.PatchValidationException;
import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import com.prasad.agentic_software_engineer.workspace.FileHashService;
import com.prasad.agentic_software_engineer.workspace.SafePathResolver;
import com.prasad.agentic_software_engineer.workspace.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatchServiceTest {

    @TempDir
    Path temporaryDirectory;

    private WorkspaceService workspaceService;
    private PatchService patchService;
    private Path allowedRoot;

    @BeforeEach
    void setUp() throws Exception {
        allowedRoot = temporaryDirectory.resolve(
                "scenario-repositories"
        );

        Files.createDirectories(allowedRoot);

        AgentWorkspaceProperties workspaceProperties =
                new AgentWorkspaceProperties(
                        temporaryDirectory.resolve(
                                "agent-workspaces"
                        )
                );

        AgentRepositoryProperties repositoryProperties =
                new AgentRepositoryProperties(
                        allowedRoot,
                        100,
                        1_000_000,
                        100_000
                );

        AgentExecutionProperties executionProperties =
                new AgentExecutionProperties(
                        2,
                        Duration.ofSeconds(30),
                        100_000,
                        20,
                        500_000
                );

        SafePathResolver resolver =
                new SafePathResolver();

        FileHashService hashes =
                new FileHashService();

        workspaceService = new WorkspaceService(
                workspaceProperties,
                repositoryProperties,
                resolver,
                hashes
        );

        patchService = new PatchService(
                new PatchPolicy(
                        executionProperties,
                        resolver,
                        hashes
                ),
                resolver,
                hashes,
                new DiffService(hashes),
                workspaceService,
                new ObjectMapper()
        );
    }

    @Test
    void appliesSourceAndTestChangesAndProducesDiff()
            throws Exception {
        createSourceRepository();

        EngineeringWorkspace workspace =
                workspaceService.create(
                        UUID.randomUUID(),
                        1,
                        Path.of("brownfield")
                );

        PatchProposal proposal =
                new PatchProposal(
                        "Generate source and test",
                        List.of(
                                create(
                                        "src/main/java/generated/Feature.java",
                                        "package generated; class Feature {}"
                                ),
                                create(
                                        "src/test/java/generated/FeatureTest.java",
                                        "package generated; class FeatureTest {}"
                                )
                        ),
                        List.of(),
                        List.of()
                );

        AppliedPatch result =
                patchService.apply(
                        workspace,
                        proposal
                );

        assertThat(result.changedFiles())
                .containsExactly(
                        "src/main/java/generated/Feature.java",
                        "src/test/java/generated/FeatureTest.java"
                );

        assertThat(result.diff())
                .contains(
                        "Feature.java",
                        "FeatureTest.java",
                        "before-sha256: NEW"
                );

        assertThat(
                workspace.repository().resolve(
                        "src/main/java/generated/Feature.java"
                )
        ).exists();

        assertThat(
                allowedRoot.resolve(
                        "brownfield/src/main/java/generated/Feature.java"
                )
        ).doesNotExist();
    }

    @Test
    void rejectsTraversalAndLeavesWorkspaceClean()
            throws Exception {
        createSourceRepository();

        EngineeringWorkspace workspace =
                workspaceService.create(
                        UUID.randomUUID(),
                        1,
                        Path.of("brownfield")
                );

        PatchProposal proposal =
                new PatchProposal(
                        "Attempt traversal",
                        List.of(
                                create(
                                        "../outside.java",
                                        "class Outside {}"
                                )
                        ),
                        List.of(),
                        List.of()
                );

        assertThatThrownBy(
                () -> patchService.apply(
                        workspace,
                        proposal
                )
        ).isInstanceOf(RuntimeException.class);

        assertThat(
                workspaceService.isClean(workspace)
        ).isTrue();
    }

    private ProposedFileChange create(
            String path,
            String content
    ) {
        return new ProposedFileChange(
                FileChangeType.CREATE,
                path,
                null,
                content,
                "Test change"
        );
    }

    private void createSourceRepository()
            throws Exception {
        Path source = allowedRoot.resolve(
                "brownfield"
        );

        Files.createDirectories(source);

        Files.writeString(
                source.resolve("pom.xml"),
                "<project/>"
        );
    }
}