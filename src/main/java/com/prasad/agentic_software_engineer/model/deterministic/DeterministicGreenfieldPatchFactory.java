package com.prasad.agentic_software_engineer.model.deterministic;

import com.prasad.agentic_software_engineer.model.FileChangeType;
import com.prasad.agentic_software_engineer.model.ModelInvocationException;
import com.prasad.agentic_software_engineer.model.PatchProposal;
import com.prasad.agentic_software_engineer.model.ProposedFileChange;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class DeterministicGreenfieldPatchFactory {

    private static final String TEMPLATE_ROOT =
            "deterministic/greenfield-url-shortener/";

    public PatchProposal implementation() {
        return new PatchProposal(
                "Create a greenfield URL-shortener HTTP service",
                List.of(
                        source("GreenfieldUrlShortenerApplication.java",
                                "Bootstraps the generated Spring application"),
                        source("CreateShortUrlRequest.java",
                                "Defines and validates the create request"),
                        source("ShortUrlResponse.java",
                                "Defines the stable API representation"),
                        source("ShortUrlService.java",
                                "Provides thread-safe URL creation and lookup"),
                        source("ShortUrlController.java",
                                "Exposes create and redirect HTTP operations"),
                        source("ApiExceptionHandler.java",
                                "Maps missing short codes to an RFC 9457 response")
                ),
                List.of(
                        "The seed supplies build configuration but no application code",
                        "In-memory storage is acceptable for this bounded greenfield scenario"
                ),
                List.of(
                        "Mappings are not durable across process restarts"
                )
        );
    }

    public PatchProposal tests() {
        return new PatchProposal(
                "Generate acceptance tests for the greenfield URL shortener",
                List.of(test("ShortUrlServiceTest.java",
                        "Verifies creation, lookup and missing-code behavior")),
                List.of("JUnit and AssertJ are supplied by the seed build"),
                List.of("HTTP integration remains part of reviewer API validation")
        );
    }

    public PatchProposal completeChange() {
        List<ProposedFileChange> changes = new ArrayList<>();
        changes.addAll(implementation().changes());
        changes.addAll(tests().changes());
        return new PatchProposal(
                "Restore the complete greenfield URL-shortener change",
                changes,
                implementation().assumptions(),
                implementation().risks()
        );
    }

    private ProposedFileChange source(String name, String rationale) {
        return change(
                "src/main/java/com/prasad/fixture/greenfield/" + name,
                name + ".template",
                rationale
        );
    }

    private ProposedFileChange test(String name, String rationale) {
        return change(
                "src/test/java/com/prasad/fixture/greenfield/" + name,
                name + ".template",
                rationale
        );
    }

    private ProposedFileChange change(
            String path,
            String template,
            String rationale
    ) {
        return new ProposedFileChange(
                FileChangeType.CREATE,
                path,
                null,
                template(template),
                rationale
        );
    }

    private String template(String name) {
        try {
            return new ClassPathResource(TEMPLATE_ROOT + name)
                    .getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new ModelInvocationException(
                    "Unable to load greenfield template: " + name,
                    exception
            );
        }
    }
}
