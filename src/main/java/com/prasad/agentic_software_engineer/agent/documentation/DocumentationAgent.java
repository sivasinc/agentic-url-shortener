package com.prasad.agentic_software_engineer.agent.documentation;

import com.prasad.agentic_software_engineer.model.DocumentationProposal;
import com.prasad.agentic_software_engineer.model.EngineeringModel;
import com.prasad.agentic_software_engineer.model.EngineeringPlan;
import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.RepositoryContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentationAgent {

    private final EngineeringModel engineeringModel;

    public DocumentationProposal generate(
            EngineeringPlan plan,
            PatchProposal validatedPatch,
            RepositoryContext repository
    ) {
        return engineeringModel.generateDocumentation(
                plan,
                validatedPatch,
                repository
        );
    }
}