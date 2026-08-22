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

    public static final String VALIDATION_RESULT =
            "validation.result";

    public static final String REPAIR_PATCH =
            "patch.repair";

    public static final String REPOSITORY_PATH =
            "repository.path";

    public static final String CLARIFICATION_DECISION =
            "governance.clarification";

    public static final String RELEASE_DECISION =
            "governance.release";

    public static final String SAFE_STOP_DECISION =
            "governance.safe-stop";

    private AgenticContextKeys() {
    }
}
