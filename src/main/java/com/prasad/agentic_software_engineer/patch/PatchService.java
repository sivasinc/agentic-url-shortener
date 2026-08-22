package com.prasad.agentic_software_engineer.patch;

import com.prasad.agentic_software_engineer.model.FileChangeType;
import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.ProposedFileChange;
import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import com.prasad.agentic_software_engineer.workspace.FileHashService;
import com.prasad.agentic_software_engineer.workspace.SafePathResolver;
import com.prasad.agentic_software_engineer.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PatchService {

    private final PatchPolicy patchPolicy;
    private final SafePathResolver safePathResolver;
    private final FileHashService fileHashService;
    private final DiffService diffService;
    private final WorkspaceService workspaceService;
    private final ObjectMapper objectMapper;

    public AppliedPatch apply(
            EngineeringWorkspace workspace,
            PatchProposal proposal
    ) {
        patchPolicy.validate(workspace, proposal);

        try {
            for (ProposedFileChange change :
                    proposal.changes()) {
                applyChange(workspace, change);
            }

            String diff =
                    diffService.generate(workspace);

            List<String> changedFiles =
                    diffService.changedFiles(workspace);

            if (changedFiles.isEmpty() ||
                    diff.isBlank()) {
                throw new PatchValidationException(
                        "Patch produced no repository changes"
                );
            }

            String artifact =
                    writeArtifacts(
                            workspace,
                            proposal,
                            diff
                    );

            Map<String, String> hashes =
                    fileHashService.manifest(
                            workspace.repository()
                    );

            return new AppliedPatch(
                    changedFiles,
                    diff,
                    hashes,
                    artifact
            );
        } catch (RuntimeException exception) {
            workspaceService.rollback(workspace);
            throw exception;
        }
    }

    private void applyChange(
            EngineeringWorkspace workspace,
            ProposedFileChange change
    ) {
        Path target = safePathResolver.resolve(
                workspace.repository(),
                change.path()
        );

        try {
            if (change.type() ==
                    FileChangeType.DELETE) {
                Files.delete(target);
                return;
            }

            Files.createDirectories(
                    target.getParent()
            );

            if (change.type() ==
                    FileChangeType.CREATE) {
                Files.writeString(
                        target,
                        change.content(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE_NEW
                );

                return;
            }

            Files.writeString(
                    target,
                    change.content(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        } catch (IOException exception) {
            throw new PatchValidationException(
                    "Unable to apply file change: " +
                            change.path(),
                    exception
            );
        }
    }

    private String writeArtifacts(
            EngineeringWorkspace workspace,
            PatchProposal proposal,
            String diff
    ) {
        Path proposalFile =
                workspace.artifacts()
                        .resolve("patch-proposal.json");

        Path diffFile =
                workspace.artifacts()
                        .resolve("final.diff");

        try {
            Files.writeString(
                    proposalFile,
                    objectMapper.writerWithDefaultPrettyPrinter()
                            .writeValueAsString(proposal),
                    StandardCharsets.UTF_8
            );

            Files.writeString(
                    diffFile,
                    diff,
                    StandardCharsets.UTF_8
            );

            return workspace.root()
                    .relativize(diffFile)
                    .toString()
                    .replace('\\', '/');
        } catch (IOException |
                 JacksonException exception) {
            throw new PatchValidationException(
                    "Unable to write patch artifacts",
                    exception
            );
        }
    }
}