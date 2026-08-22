package com.prasad.agentic_software_engineer.orchestration.engine;

public final class AgenticContextKeys {

    public static final String REQUIREMENT_CONTEXT =
            "requirement.context";

    public static final String REQUIREMENT_ANALYSIS =
            "requirement.analysis";

    public static final String WORKSPACE =
            "workspace";

    public static final String REPOSITORY_ASSESSMENT =
            "repository.assessment";

    public static final String REPOSITORY_CONTEXT =
            "repository.context";

    public static final String ENGINEERING_PLAN =
            "engineering.plan";

    public static final String IMPLEMENTATION_PATCH =
            "patch.implementation";

    public static final String TEST_PATCH =
            "patch.tests";

    public static final String APPLIED_PATCH =
            "patch.applied";

    private AgenticContextKeys() {
    }
}