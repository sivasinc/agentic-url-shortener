package com.prasad.agentic_software_engineer.orchestration.controller;

import com.prasad.agentic_software_engineer.audit.AgentAuditEvent;
import com.prasad.agentic_software_engineer.audit.AuditEvidence;
import com.prasad.agentic_software_engineer.audit.WorkflowAuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/engineering-workflows/{workflowId}")
public class WorkflowEvidenceController {

    private final WorkflowAuditService auditService;

    @GetMapping("/audit-events")
    public List<AgentAuditEvent> auditEvents(
            @PathVariable UUID workflowId
    ) {
        return auditService.events(workflowId);
    }

    @GetMapping("/evidence")
    public List<AuditEvidence> evidence(
            @PathVariable UUID workflowId
    ) {
        return auditService.evidence(workflowId);
    }
}
