package com.prasad.agentic_software_engineer.orchestration.governance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SubmitClarificationRequest(
        @NotBlank
        @Size(max = 200)
        String actor,

        @NotEmpty
        @Size(max = 20)
        List<@NotBlank @Size(max = 2000) String> answers
) {
}
