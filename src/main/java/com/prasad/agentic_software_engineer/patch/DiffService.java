package com.prasad.agentic_software_engineer.patch;

import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import com.prasad.agentic_software_engineer.workspace.FileHashService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@Component
@RequiredArgsConstructor
public class DiffService {

    private final FileHashService fileHashService;

    public String generate(
            EngineeringWorkspace workspace
    ) {
        Map<String, String> baselineHashes =
                workspace.baselineHashes();

        Map<String, String> currentHashes =
                fileHashService.manifest(
                        workspace.repository()
                );

        Set<String> paths = new TreeSet<>();

        paths.addAll(baselineHashes.keySet());
        paths.addAll(currentHashes.keySet());

        StringBuilder diff = new StringBuilder();

        for (String path : paths) {
            String beforeHash =
                    baselineHashes.get(path);

            String afterHash =
                    currentHashes.get(path);

            if (beforeHash != null &&
                    beforeHash.equals(afterHash)) {
                continue;
            }

            appendFileDiff(
                    diff,
                    workspace,
                    path,
                    beforeHash,
                    afterHash
            );
        }

        return diff.toString();
    }

    public List<String> changedFiles(
            EngineeringWorkspace workspace
    ) {
        Map<String, String> currentHashes =
                fileHashService.manifest(
                        workspace.repository()
                );

        Set<String> paths = new TreeSet<>();

        paths.addAll(
                workspace.baselineHashes().keySet()
        );

        paths.addAll(currentHashes.keySet());

        return paths.stream()
                .filter(
                        path ->
                                !java.util.Objects.equals(
                                        workspace
                                                .baselineHashes()
                                                .get(path),
                                        currentHashes.get(path)
                                )
                )
                .toList();
    }

    private void appendFileDiff(
            StringBuilder diff,
            EngineeringWorkspace workspace,
            String relativePath,
            String beforeHash,
            String afterHash
    ) {
        diff.append("diff --agentic a/")
                .append(relativePath)
                .append(" b/")
                .append(relativePath)
                .append(System.lineSeparator());

        diff.append("before-sha256: ")
                .append(
                        beforeHash == null
                                ? "NEW"
                                : beforeHash
                )
                .append(System.lineSeparator());

        diff.append("after-sha256: ")
                .append(
                        afterHash == null
                                ? "DELETED"
                                : afterHash
                )
                .append(System.lineSeparator());

        diff.append("--- ")
                .append(
                        beforeHash == null
                                ? "/dev/null"
                                : "a/" + relativePath
                )
                .append(System.lineSeparator());

        diff.append("+++ ")
                .append(
                        afterHash == null
                                ? "/dev/null"
                                : "b/" + relativePath
                )
                .append(System.lineSeparator());

        appendContent(
                diff,
                workspace.baseline().resolve(
                        relativePath
                ),
                '-'
        );

        appendContent(
                diff,
                workspace.repository().resolve(
                        relativePath
                ),
                '+'
        );

        diff.append(System.lineSeparator());
    }

    private void appendContent(
            StringBuilder diff,
            Path file,
            char prefix
    ) {
        if (!Files.isRegularFile(file)) {
            return;
        }

        try {
            byte[] bytes = Files.readAllBytes(file);

            if (isBinary(bytes)) {
                diff.append(prefix)
                        .append("[binary content]")
                        .append(System.lineSeparator());

                return;
            }

            String content = new String(
                    bytes,
                    StandardCharsets.UTF_8
            );

            List<String> lines =
                    new ArrayList<>(
                            content.lines().toList()
                    );

            for (String line : lines) {
                diff.append(prefix)
                        .append(line)
                        .append(System.lineSeparator());
            }
        } catch (IOException exception) {
            throw new PatchValidationException(
                    "Unable to generate diff for " +
                            file,
                    exception
            );
        }
    }

    private boolean isBinary(byte[] bytes) {
        int inspected = Math.min(
                bytes.length,
                4096
        );

        for (int index = 0;
             index < inspected;
             index++) {
            if (bytes[index] == 0) {
                return true;
            }
        }

        return false;
    }
}