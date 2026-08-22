package com.prasad.agentic_software_engineer.orchestration.governance;

import com.prasad.agentic_software_engineer.orchestration.dto.EngineeringWorkflowResponse;
import com.prasad.agentic_software_engineer.orchestration.service.AgenticWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/engineering-workflows/{workflowId}")
public class WorkflowGovernanceController {

    private final AgenticWorkflowService workflowService;

    @PostMapping("/clarifications")
    public EngineeringWorkflowResponse clarify(
            @PathVariable UUID workflowId,
            @Valid @RequestBody SubmitClarificationRequest request
    ) {
        return workflowService.clarify(
                workflowId,
                request.actor(),
                request.answers()
        );
    }

    @PostMapping("/approvals/release-readiness")
    public EngineeringWorkflowResponse decideRelease(
            @PathVariable UUID workflowId,
            @Valid @RequestBody ReleaseApprovalRequest request
    ) {
        return workflowService.decideRelease(
                workflowId,
                request.actor(),
                request.approved(),
                request.reason()
        );
    }

    @PostMapping("/safe-stop")
    public EngineeringWorkflowResponse safeStop(
            @PathVariable UUID workflowId,
            @Valid @RequestBody SafeStopRequest request
    ) {
        return workflowService.safeStop(
                workflowId,
                request.actor(),
                request.reason()
        );
    }
}
