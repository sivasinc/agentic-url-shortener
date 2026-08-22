package com.prasad.agentic_software_engineer.agent.repository;

import com.prasad.agentic_software_engineer.config.AgentRepositoryProperties;
import com.prasad.agentic_software_engineer.model.RepositoryContext;
import com.prasad.agentic_software_engineer.model.RequirementAnalysis;
import com.prasad.agentic_software_engineer.tool.repository.RepositoryTools;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RepositoryContextAssembler {

    private final RepositoryTools repositoryTools;
    private final AgentRepositoryProperties properties;

    public RepositoryContext assemble(
            Path repositoryRoot,
            RequirementAnalysis requirement,
            RepositoryAssessment assessment
    ) {
        Set<String> candidates =
                new LinkedHashSet<>(
                        assessment.impactedFiles()
                );

        assessment.sourceFiles()
                .stream()
                .limit(10)
                .forEach(candidates::add);

        assessment.testFiles()
                .stream()
                .limit(10)
                .forEach(candidates::add);

        assessment.migrations()
                .stream()
                .limit(5)
                .forEach(candidates::add);

        Map<String, String> relevantFiles =
                new LinkedHashMap<>();

        int characters = 0;

        for (String path : candidates) {
            String content;

            try {
                content = repositoryTools.readFile(
                        repositoryRoot,
                        path
                );
            } catch (RuntimeException exception) {
                continue;
            }

            if (characters + content.length() >
                    properties.maxContextCharacters()) {
                break;
            }

            relevantFiles.put(path, content);
            characters += content.length();
        }

        return new RepositoryContext(
                requirement,
                assessment,
                relevantFiles
        );
    }
}