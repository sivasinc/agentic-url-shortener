package com.prasad.agentic_software_engineer.model;

public interface EngineeringModel {

    RequirementAnalysis analyzeRequirement(
            RequirementContext context
    );

    EngineeringPlan createPlan(
            RepositoryContext repository
    );

    PatchProposal generateImplementation(
            EngineeringPlan plan,
            RepositoryContext repository
    );

    PatchProposal generateTests(
            EngineeringPlan plan,
            PatchProposal implementation,
            RepositoryContext repository
    );

    PatchProposal repair(
            EngineeringPlan plan,
            PatchProposal previousPatch,
            ValidationFailure failure,
            RepositoryContext repository
    );

    DocumentationProposal generateDocumentation(
            EngineeringPlan plan,
            PatchProposal validatedPatch,
            RepositoryContext repository
    );
}