package com.prasad.agentic_software_engineer.validation;

import com.prasad.agentic_software_engineer.config.AgentExecutionProperties;
import com.prasad.agentic_software_engineer.workspace.EngineeringWorkspace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class MavenBuildTool {

    private static final List<String> MAVEN_ARGUMENTS =
            List.of(
                    "--batch-mode",
                    "--no-transfer-progress",
                    "clean",
                    "test"
            );

    private final AgentExecutionProperties properties;

    public BuildValidationResult validate(
            EngineeringWorkspace workspace,
            int attempt
    ) {
        if (workspace == null) {
            throw new BuildExecutionException(
                    "Workspace is required"
            );
        }

        if (attempt < 1) {
            throw new BuildExecutionException(
                    "Validation attempt must be positive"
            );
        }

        Path repository = workspace.repository()
                .toAbsolutePath()
                .normalize();

        List<String> command =
                resolveCommand(repository);

        Instant startedAt = Instant.now();

        Process process = null;

        try {
            process = new ProcessBuilder(command)
                    .directory(repository.toFile())
                    .redirectErrorStream(true)
                    .start();

            Process activeProcess = process;

            CompletableFuture<String> outputFuture =
                    CompletableFuture.supplyAsync(
                            () -> readOutput(activeProcess)
                    );

            boolean completed = process.waitFor(
                    properties.commandTimeout().toMillis(),
                    TimeUnit.MILLISECONDS
            );

            boolean timedOut = !completed;

            if (timedOut) {
                terminate(process);
            }

            String output = outputFuture.join();

            int exitCode = completed
                    ? process.exitValue()
                    : -1;

            Duration duration =
                    Duration.between(
                            startedAt,
                            Instant.now()
                    );

            String boundedOutput =
                    boundOutput(output);

            String logArtifact = writeLog(
                    workspace,
                    attempt,
                    command,
                    exitCode,
                    timedOut,
                    duration,
                    boundedOutput
            );

            return new BuildValidationResult(
                    completed && exitCode == 0,
                    String.join(" ", command),
                    exitCode,
                    timedOut,
                    duration,
                    boundedOutput,
                    logArtifact
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            if (process != null) {
                terminate(process);
            }

            throw new BuildExecutionException(
                    "Maven validation was interrupted",
                    exception
            );
        } catch (IOException exception) {
            if (process != null) {
                terminate(process);
            }

            throw new BuildExecutionException(
                    "Unable to execute Maven validation",
                    exception
            );
        }
    }

    private List<String> resolveCommand(
            Path repository
    ) {
        boolean windows = System.getProperty("os.name")
                .toLowerCase(Locale.ROOT)
                .contains("win");

        if (windows) {
            Path wrapper =
                    repository.resolve("mvnw.cmd");

            if (!Files.isRegularFile(wrapper)) {
                throw new BuildExecutionException(
                        "Repository does not contain mvnw.cmd"
                );
            }

            List<String> command =
                    new ArrayList<>();

            command.add("cmd.exe");
            command.add("/d");
            command.add("/s");
            command.add("/c");
            command.add("mvnw.cmd");
            command.addAll(MAVEN_ARGUMENTS);

            return List.copyOf(command);
        }

        Path wrapper = repository.resolve("mvnw");

        if (!Files.isRegularFile(wrapper)) {
            throw new BuildExecutionException(
                    "Repository does not contain mvnw"
            );
        }

        if (!Files.isExecutable(wrapper)) {
            throw new BuildExecutionException(
                    "Repository Maven wrapper is not executable"
            );
        }

        List<String> command =
                new ArrayList<>();

        command.add("./mvnw");
        command.addAll(MAVEN_ARGUMENTS);

        return List.copyOf(command);
    }

    private String readOutput(Process process) {
        StringBuilder output =
                new StringBuilder();

        try (BufferedReader reader =
                     process.inputReader(
                             StandardCharsets.UTF_8
                     )) {
            String line;

            while ((line = reader.readLine()) != null) {
                if (output.length() <
                        properties.maxOutputCharacters()) {
                    int remaining =
                            properties.maxOutputCharacters() -
                                    output.length();

                    output.append(
                            line,
                            0,
                            Math.min(
                                    line.length(),
                                    remaining
                            )
                    );

                    if (output.length() <
                            properties.maxOutputCharacters()) {
                        output.append(
                                System.lineSeparator()
                        );
                    }
                }
            }

            return output.toString();
        } catch (IOException exception) {
            throw new BuildExecutionException(
                    "Unable to read Maven output",
                    exception
            );
        }
    }

    private String boundOutput(String output) {
        if (output.length() <=
                properties.maxOutputCharacters()) {
            return output;
        }

        return output.substring(
                output.length() -
                        properties.maxOutputCharacters()
        );
    }

    private String writeLog(
            EngineeringWorkspace workspace,
            int attempt,
            List<String> command,
            int exitCode,
            boolean timedOut,
            Duration duration,
            String output
    ) {
        Path logFile = workspace.logs()
                .resolve(
                        "maven-test-attempt-" +
                                attempt +
                                ".log"
                );

        String content = """
                command=%s
                exitCode=%d
                timedOut=%s
                durationMillis=%d

                %s
                """.formatted(
                String.join(" ", command),
                exitCode,
                timedOut,
                duration.toMillis(),
                output
        );

        try {
            Files.writeString(
                    logFile,
                    content,
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new BuildExecutionException(
                    "Unable to write Maven validation log",
                    exception
            );
        }

        return workspace.root()
                .relativize(logFile)
                .toString()
                .replace('\\', '/');
    }

    private void terminate(Process process) {
        process.descendants()
                .forEach(
                        descendant ->
                                descendant.destroyForcibly()
                );

        process.destroyForcibly();
    }
}