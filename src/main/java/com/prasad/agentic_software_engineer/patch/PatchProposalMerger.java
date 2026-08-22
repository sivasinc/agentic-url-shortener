package com.prasad.agentic_software_engineer.patch;

import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.ProposedFileChange;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class PatchProposalMerger {

    public PatchProposal merge(
            PatchProposal implementation,
            PatchProposal tests
    ) {
        List<ProposedFileChange> changes =
                new ArrayList<>();

        changes.addAll(implementation.changes());
        changes.addAll(tests.changes());

        Set<String> paths = new HashSet<>();

        for (ProposedFileChange change : changes) {
            String normalized = change.path()
                    .replace('\\', '/');

            if (!paths.add(normalized)) {
                throw new PatchValidationException(
                        "Multiple changes target the same file: " +
                                normalized
                );
            }
        }

        List<String> assumptions =
                new ArrayList<>();

        assumptions.addAll(
                implementation.assumptions()
        );

        assumptions.addAll(
                tests.assumptions()
        );

        List<String> risks = new ArrayList<>();

        risks.addAll(implementation.risks());
        risks.addAll(tests.risks());

        return new PatchProposal(
                implementation.summary() +
                        "; " +
                        tests.summary(),
                changes,
                assumptions,
                risks
        );
    }
}