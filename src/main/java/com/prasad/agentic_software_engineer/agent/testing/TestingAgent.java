package com.prasad.agentic_software_engineer.agent.testing;

import com.prasad.agentic_software_engineer.model.EngineeringModel;
import com.prasad.agentic_software_engineer.model.EngineeringPlan;
import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.RepositoryContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TestingAgent {

    private final EngineeringModel engineeringModel;

    public PatchProposal generate(
            EngineeringPlan plan,
            PatchProposal implementation,
            RepositoryContext repository
    ) {
        return engineeringModel.generateTests(
                plan,
                implementation,
                repository
        );
    }
}