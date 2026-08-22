package com.prasad.agentic_software_engineer.orchestration.governance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReleaseApprovalRequest(
        @NotBlank
        @Size(max = 200)
        String actor,

        @NotNull
        Boolean approved,

        @NotBlank
        @Size(max = 2000)
        String reason
) {
}
