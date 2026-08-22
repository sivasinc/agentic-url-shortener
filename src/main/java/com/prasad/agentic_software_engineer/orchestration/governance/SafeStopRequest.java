package com.prasad.agentic_software_engineer.orchestration.governance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SafeStopRequest(
        @NotBlank
        @Size(max = 200)
        String actor,

        @NotBlank
        @Size(max = 2000)
        String reason
) {
}
