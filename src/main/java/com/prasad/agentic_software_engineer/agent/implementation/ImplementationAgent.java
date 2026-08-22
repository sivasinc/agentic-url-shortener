package com.prasad.agentic_software_engineer.agent.implementation;

import com.prasad.agentic_software_engineer.model.EngineeringModel;
import com.prasad.agentic_software_engineer.model.EngineeringPlan;
import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.RepositoryContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImplementationAgent {

    private final EngineeringModel engineeringModel;

    public PatchProposal generate(
            EngineeringPlan plan,
            RepositoryContext repository
    ) {
        return engineeringModel.generateImplementation(
                plan,
                repository
        );
    }
}