package com.prasad.agentic_software_engineer.agent.architecture;

import com.prasad.agentic_software_engineer.model.EngineeringModel;
import com.prasad.agentic_software_engineer.model.EngineeringPlan;
import com.prasad.agentic_software_engineer.model.RepositoryContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ArchitectureAgent {

    private final EngineeringModel engineeringModel;

    public EngineeringPlan plan(
            RepositoryContext repository
    ) {
        return engineeringModel.createPlan(repository);
    }
}