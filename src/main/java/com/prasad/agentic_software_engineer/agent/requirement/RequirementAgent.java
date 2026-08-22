package com.prasad.agentic_software_engineer.agent.requirement;

import com.prasad.agentic_software_engineer.model.EngineeringModel;
import com.prasad.agentic_software_engineer.model.RequirementAnalysis;
import com.prasad.agentic_software_engineer.model.RequirementContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequirementAgent {

    private final EngineeringModel engineeringModel;

    public RequirementAnalysis analyze(
            RequirementContext context
    ) {
        return engineeringModel
                .analyzeRequirement(context);
    }
}