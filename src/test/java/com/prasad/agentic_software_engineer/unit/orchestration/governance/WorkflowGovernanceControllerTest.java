package com.prasad.agentic_software_engineer.unit.orchestration.governance;

import com.prasad.agentic_software_engineer.orchestration.domain.WorkflowStatus;
import com.prasad.agentic_software_engineer.orchestration.dto.EngineeringWorkflowResponse;
import com.prasad.agentic_software_engineer.orchestration.governance.ReleaseApprovalRequest;
import com.prasad.agentic_software_engineer.orchestration.governance.SafeStopRequest;
import com.prasad.agentic_software_engineer.orchestration.governance.SubmitClarificationRequest;
import com.prasad.agentic_software_engineer.orchestration.governance.WorkflowGovernanceController;
import com.prasad.agentic_software_engineer.orchestration.service.AgenticWorkflowService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowGovernanceControllerTest {

    private final AgenticWorkflowService service =
            mock(AgenticWorkflowService.class);

    private final WorkflowGovernanceController controller =
            new WorkflowGovernanceController(service);

    @Test
    void submitsClarificationToWorkflowService() {
        UUID workflowId = UUID.randomUUID();
        EngineeringWorkflowResponse expected = response(
                workflowId,
                WorkflowStatus.CREATED
        );

        when(service.clarify(
                workflowId,
                "developer@example.com",
                List.of("Track daily redirects in UTC")
        )).thenReturn(expected);

        EngineeringWorkflowResponse actual = controller.clarify(
                workflowId,
                new SubmitClarificationRequest(
                        "developer@example.com",
                        List.of("Track daily redirects in UTC")
                )
        );

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void submitsReleaseDecisionToWorkflowService() {
        UUID workflowId = UUID.randomUUID();
        EngineeringWorkflowResponse expected = response(
                workflowId,
                WorkflowStatus.RUNNING
        );

        when(service.decideRelease(
                workflowId,
                "reviewer@example.com",
                true,
                "Evidence reviewed"
        )).thenReturn(expected);

        EngineeringWorkflowResponse actual = controller.decideRelease(
                workflowId,
                new ReleaseApprovalRequest(
                        "reviewer@example.com",
                        true,
                        "Evidence reviewed"
                )
        );

        assertThat(actual).isSameAs(expected);
    }

    @Test
    void submitsSafeStopToWorkflowService() {
        UUID workflowId = UUID.randomUUID();
        EngineeringWorkflowResponse expected = response(
                workflowId,
                WorkflowStatus.SAFE_STOPPED
        );

        when(service.safeStop(
                workflowId,
                "operator@example.com",
                "Controlled termination"
        )).thenReturn(expected);

        EngineeringWorkflowResponse actual = controller.safeStop(
                workflowId,
                new SafeStopRequest(
                        "operator@example.com",
                        "Controlled termination"
                )
        );

        assertThat(actual).isSameAs(expected);
        verify(service).safeStop(
                workflowId,
                "operator@example.com",
                "Controlled termination"
        );
    }

    private EngineeringWorkflowResponse response(
            UUID workflowId,
            WorkflowStatus status
    ) {
        return new EngineeringWorkflowResponse(
                workflowId,
                1,
                status,
                "deterministic",
                null,
                null,
                List.of(),
                null,
                null,
                List.of(),
                List.of(),
                null
        );
    }
}
