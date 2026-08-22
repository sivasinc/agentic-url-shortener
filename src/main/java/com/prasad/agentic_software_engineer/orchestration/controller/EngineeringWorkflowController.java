package com.prasad.agentic_software_engineer.orchestration.controller;

import com.prasad.agentic_software_engineer.orchestration.dto.CreateEngineeringWorkflowRequest;
import com.prasad.agentic_software_engineer.orchestration.dto.EngineeringWorkflowResponse;
import com.prasad.agentic_software_engineer.orchestration.service.AgenticWorkflowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/engineering-workflows")
public class EngineeringWorkflowController {

    private final AgenticWorkflowService workflowService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EngineeringWorkflowResponse create(
            @Valid
            @RequestBody
            CreateEngineeringWorkflowRequest request
    ) {
        return workflowService.create(request);
    }

    @GetMapping("/{workflowId}")
    public EngineeringWorkflowResponse get(
            @PathVariable UUID workflowId
    ) {
        return workflowService.get(workflowId);
    }
}