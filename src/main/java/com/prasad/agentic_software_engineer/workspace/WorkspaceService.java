package com.prasad.agentic_software_engineer.workspace;

import com.prasad.agentic_software_engineer.config.AgentRepositoryProperties;
import com.prasad.agentic_software_engineer.config.AgentWorkspaceProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class WorkspaceService {

    private final AgentWorkspaceProperties workspaceProperties;
    private final AgentRepositoryProperties repositoryProperties;
    private final SafePathResolver safePathResolver;
    private final FileHashService fileHashService;

    public EngineeringWorkspace create(
            UUID workflowId,
            long revision,
            Path sourceRepository
    ) {
        if (workflowId == null) {
            throw new WorkspaceException(
                    "Workflow ID is required"
            );
        }

        if (revision < 1) {
            throw new WorkspaceException(
                    "Workflow revision must be positive"
            );
        }

        Path approvedSource =
                resolveApprovedSource(sourceRepository);

        Path workspaceRoot = workspaceProperties.root()
                .toAbsolutePath()
                .normalize()
                .resolve(workflowId.toString())
                .resolve("revision-" + revision);

        Path repository =
                workspaceRoot.resolve("repository");

        Path baseline =
                workspaceRoot.resolve("snapshots")
                        .resolve("baseline");

        Path artifacts =
                workspaceRoot.resolve("artifacts");

        Path logs =
                workspaceRoot.resolve("logs");

        if (Files.exists(workspaceRoot)) {
            throw new WorkspaceException(
                    "Workspace already exists for workflow " +
                            workflowId +
                            " revision " +
                            revision
            );
        }

        try {
            Files.createDirectories(workspaceRoot);
            Files.createDirectories(artifacts);
            Files.createDirectories(logs);

            copyRepository(
                    approvedSource,
                    repository,
                    true
            );

            copyRepository(
                    repository,
                    baseline,
                    false
            );

            Map<String, String> baselineHashes =
                    fileHashService.manifest(repository);

            return new EngineeringWorkspace(
                    workflowId,
                    revision,
                    workspaceRoot,
                    repository,
                    baseline,
                    artifacts,
                    logs,
                    baselineHashes
            );
        } catch (RuntimeException | IOException exception) {
            deleteFailedWorkspace(workspaceRoot);

            if (exception instanceof WorkspaceException workspaceException) {
                throw workspaceException;
            }

            throw new WorkspaceException(
                    "Unable to create engineering workspace",
                    exception
            );
        }
    }

    public void rollback(EngineeringWorkspace workspace) {
        validateManagedWorkspace(workspace);

        deleteRecursively(workspace.repository());

        copyRepository(
                workspace.baseline(),
                workspace.repository(),
                false
        );

        Map<String, String> restoredHashes =
                fileHashService.manifest(
                        workspace.repository()
                );

        if (!restoredHashes.equals(
                workspace.baselineHashes()
        )) {
            throw new WorkspaceException(
                    "Rollback verification failed"
            );
        }
    }

    public boolean isClean(
            EngineeringWorkspace workspace
    ) {
        validateManagedWorkspace(workspace);

        return fileHashService
                .manifest(workspace.repository())
                .equals(workspace.baselineHashes());
    }

    private Path resolveApprovedSource(
            Path sourceRepository
    ) {
        if (sourceRepository == null) {
            throw new WorkspaceException(
                    "Source repository is required"
            );
        }

        if (sourceRepository.isAbsolute()) {
            throw new WorkspaceException(
                    "Source repository must be relative to " +
                            "the approved repository root"
            );
        }

        Path allowedRoot = repositoryProperties
                .allowedRoot()
                .toAbsolutePath()
                .normalize();

        if (!Files.isDirectory(allowedRoot)) {
            throw new WorkspaceException(
                    "Approved repository root does not exist: " +
                            allowedRoot
            );
        }

        Path source = safePathResolver.resolve(
                allowedRoot,
                sourceRepository
        );

        if (!Files.isDirectory(source)) {
            throw new WorkspaceException(
                    "Source repository does not exist: " +
                            sourceRepository
            );
        }

        try {
            Path realAllowedRoot =
                    allowedRoot.toRealPath();

            Path realSource = source.toRealPath();

            if (!realSource.startsWith(realAllowedRoot)) {
                throw new WorkspaceException(
                        "Source repository escapes approved root"
                );
            }

            return realSource;
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Unable to verify source repository",
                    exception
            );
        }
    }

    private void copyRepository(
            Path source,
            Path target,
            boolean enforceRepositoryLimits
    ) {
        AtomicInteger fileCount =
                new AtomicInteger();

        try (Stream<Path> paths = Files.walk(source)) {
            paths.sorted()
                    .forEach(
                            path -> copyPath(
                                    source,
                                    target,
                                    path,
                                    fileCount,
                                    enforceRepositoryLimits
                            )
                    );
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Unable to copy repository",
                    exception
            );
        }
    }

    private void copyPath(
            Path sourceRoot,
            Path targetRoot,
            Path sourcePath,
            AtomicInteger fileCount,
            boolean enforceRepositoryLimits
    ) {
        if (Files.isSymbolicLink(sourcePath)) {
            throw new WorkspaceException(
                    "Symbolic links are not allowed: " +
                            sourceRoot.relativize(sourcePath)
            );
        }

        Path relative = sourceRoot.relativize(
                sourcePath
        );

        Path target = targetRoot
                .resolve(relative)
                .normalize();

        if (!target.startsWith(
                targetRoot.toAbsolutePath().normalize()
        )) {
            throw new WorkspaceException(
                    "Copy target escapes workspace"
            );
        }

        try {
            if (Files.isDirectory(sourcePath)) {
                Files.createDirectories(target);
                return;
            }

            if (!Files.isRegularFile(sourcePath)) {
                return;
            }

            if (enforceRepositoryLimits) {
                enforceFileLimits(
                        sourcePath,
                        fileCount.incrementAndGet()
                );
            }

            Files.createDirectories(target.getParent());

            Files.copy(
                    sourcePath,
                    target,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Unable to copy file: " + relative,
                    exception
            );
        }
    }

    private void enforceFileLimits(
            Path file,
            int fileCount
    ) throws IOException {
        if (fileCount > repositoryProperties.maxFiles()) {
            throw new WorkspaceException(
                    "Repository exceeds maximum file count of " +
                            repositoryProperties.maxFiles()
            );
        }

        long size = Files.size(file);

        if (size > repositoryProperties.maxFileSizeBytes()) {
            throw new WorkspaceException(
                    "Repository file exceeds maximum size: " +
                            file
            );
        }
    }

    private void validateManagedWorkspace(
            EngineeringWorkspace workspace
    ) {
        if (workspace == null) {
            throw new WorkspaceException(
                    "Workspace is required"
            );
        }

        Path configuredRoot = workspaceProperties.root()
                .toAbsolutePath()
                .normalize();

        Path workspaceRoot = workspace.root()
                .toAbsolutePath()
                .normalize();

        Path expectedRepository =
                workspaceRoot.resolve("repository");

        Path expectedBaseline =
                workspaceRoot.resolve("snapshots")
                        .resolve("baseline");

        if (!workspaceRoot.startsWith(configuredRoot) ||
                !workspace.repository()
                        .toAbsolutePath()
                        .normalize()
                        .equals(expectedRepository) ||
                !workspace.baseline()
                        .toAbsolutePath()
                        .normalize()
                        .equals(expectedBaseline)) {
            throw new WorkspaceException(
                    "Workspace paths are not managed by this service"
            );
        }
    }

    private void deleteFailedWorkspace(
            Path workspaceRoot
    ) {
        if (Files.exists(workspaceRoot)) {
            deleteRecursively(workspaceRoot);
        }
    }

    private void deleteRecursively(Path target) {
        Path configuredRoot = workspaceProperties.root()
                .toAbsolutePath()
                .normalize();

        Path normalizedTarget = target
                .toAbsolutePath()
                .normalize();

        if (!normalizedTarget.startsWith(configuredRoot) ||
                normalizedTarget.equals(configuredRoot)) {
            throw new WorkspaceException(
                    "Refusing to delete unmanaged path: " +
                            normalizedTarget
            );
        }

        if (!Files.exists(normalizedTarget)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(normalizedTarget)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(this::deletePath);
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Unable to delete workspace path",
                    exception
            );
        }
    }

    private void deletePath(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Unable to delete workspace path: " +
                            path,
                    exception
            );
        }
    }
}