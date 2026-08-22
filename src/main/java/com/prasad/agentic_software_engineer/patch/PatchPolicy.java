package com.prasad.agentic_software_engineer.patch;

import com.prasad.agentic_software_engineer.config.AgentExecutionProperties;
import com.prasad.agentic_software_engineer.model.FileChangeType;
import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.ProposedFileChange;
import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import com.prasad.agentic_software_engineer.workspace.FileHashService;
import com.prasad.agentic_software_engineer.workspace.SafePathResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class PatchPolicy {

    private static final Set<String> PROTECTED_PATHS =
            Set.of(
                    ".env",
                    "application-secrets.yml",
                    "application-secrets.yaml"
            );

    private final AgentExecutionProperties properties;
    private final SafePathResolver safePathResolver;
    private final FileHashService fileHashService;

    public void validate(
            EngineeringWorkspace workspace,
            PatchProposal proposal
    ) {
        if (proposal.changes().size() >
                properties.maxPatchFiles()) {
            throw new PatchValidationException(
                    "Patch exceeds maximum file count of " +
                            properties.maxPatchFiles()
            );
        }

        long totalBytes = 0;
        Set<String> paths = new HashSet<>();

        for (ProposedFileChange change :
                proposal.changes()) {
            String normalizedPath =
                    normalize(change.path());

            if (!paths.add(normalizedPath)) {
                throw new PatchValidationException(
                        "Duplicate patch path: " +
                                normalizedPath
                );
            }

            rejectProtectedPath(normalizedPath);

            Path target = safePathResolver.resolve(
                    workspace.repository(),
                    normalizedPath
            );

            if (change.content() != null) {
                totalBytes += change.content()
                        .getBytes(StandardCharsets.UTF_8)
                        .length;
            }

            if (totalBytes >
                    properties.maxPatchBytes()) {
                throw new PatchValidationException(
                        "Patch exceeds maximum size of " +
                                properties.maxPatchBytes() +
                                " bytes"
                );
            }

            validateOperation(change, target);
        }
    }

    private void validateOperation(
            ProposedFileChange change,
            Path target
    ) {
        if (change.type() ==
                FileChangeType.CREATE) {
            if (Files.exists(target)) {
                throw new PatchValidationException(
                        "CREATE target already exists: " +
                                change.path()
                );
            }

            return;
        }

        if (!Files.isRegularFile(target)) {
            throw new PatchValidationException(
                    "Patch target does not exist: " +
                            change.path()
            );
        }

        String currentHash =
                fileHashService.sha256(target);

        if (!currentHash.equalsIgnoreCase(
                change.expectedSha256()
        )) {
            throw new PatchValidationException(
                    "Expected hash does not match current file: " +
                            change.path()
            );
        }
    }

    private void rejectProtectedPath(String path) {
        String lower = path.toLowerCase(
                Locale.ROOT
        );

        if (PROTECTED_PATHS.contains(lower) ||
                lower.startsWith(".git/") ||
                lower.equals(".git") ||
                lower.startsWith("target/") ||
                lower.startsWith("agent-workspaces/") ||
                lower.contains("/../")) {
            throw new PatchValidationException(
                    "Patch targets a protected path: " +
                            path
            );
        }
    }

    private String normalize(String path) {
        return path.replace('\\', '/');
    }
}