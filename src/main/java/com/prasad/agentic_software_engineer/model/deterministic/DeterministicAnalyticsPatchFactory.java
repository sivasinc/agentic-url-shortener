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
public class DeterministicAnalyticsPatchFactory {

    private static final String TEMPLATE_ROOT =
            "deterministic/url-analytics/";

    public PatchProposal implementation() {
        return new PatchProposal(
                "Add persistent total and daily redirect analytics",
                List.of(
                        source(
                                "RedirectEvent.java",
                                "RedirectEvent.java.template",
                                "Persists one immutable event for every redirect"
                        ),
                        source(
                                "RedirectEventRepository.java",
                                "RedirectEventRepository.java.template",
                                "Provides redirect-event persistence and lookup"
                        ),
                        source(
                                "RedirectAnalyticsResponse.java",
                                "RedirectAnalyticsResponse.java.template",
                                "Defines the analytics API response"
                        ),
                        source(
                                "RedirectAnalyticsService.java",
                                "RedirectAnalyticsService.java.template",
                                "Calculates total and daily redirect counts"
                        ),
                        source(
                                "RedirectAnalyticsController.java",
                                "RedirectAnalyticsController.java.template",
                                "Exposes redirect analytics through a versioned API"
                        ),
                        source(
                                "RedirectAnalyticsInterceptor.java",
                                "RedirectAnalyticsInterceptor.java.template",
                                "Observes successful redirect responses"
                        ),
                        source(
                                "AnalyticsWebConfiguration.java",
                                "AnalyticsWebConfiguration.java.template",
                                "Registers redirect observation with Spring MVC"
                        )
                ),
                List.of(
                        "The brownfield fixture uses eight-character short codes",
                        "Redirect events are retained for the lifetime of the fixture database"
                ),
                List.of(
                        "Reading all events for daily aggregation requires pagination at larger scale"
                )
        );
    }

    public PatchProposal tests() {
        return new PatchProposal(
                "Add unit coverage for total and daily redirect analytics",
                List.of(
                        new ProposedFileChange(
                                FileChangeType.CREATE,
                                "src/test/java/com/prasad/fixture/" +
                                        "url_shortener/analytics/" +
                                        "RedirectAnalyticsServiceTest.java",
                                null,
                                template(
                                        "RedirectAnalyticsServiceTest.java.template"
                                ),
                                "Verifies redirect recording and daily aggregation"
                        )
                ),
                List.of(
                        "The fixture already provides JUnit, Mockito and AssertJ"
                ),
                List.of(
                        "Controller integration coverage is deferred to the live scenario"
                )
        );
    }

    public PatchProposal intentionallyFailingImplementation() {
        PatchProposal complete = implementation();

        List<ProposedFileChange> changes = complete.changes()
                .stream()
                .map(change -> {
                    if (!change.path().endsWith(
                            "RedirectAnalyticsService.java"
                    )) {
                        return change;
                    }

                    return new ProposedFileChange(
                            change.type(),
                            change.path(),
                            change.expectedSha256(),
                            change.content().replace(
                                    "return new RedirectAnalyticsResponse(",
                                    "return unresolvedRepairDemoSymbol("
                            ),
                            "Intentionally introduces a compilation failure " +
                                    "to demonstrate bounded repair"
                    );
                })
                .toList();

        return new PatchProposal(
                "Introduce a controlled compilation failure for repair demonstration",
                changes,
                complete.assumptions(),
                List.of(
                        "The first validation attempt is expected to fail by design"
                )
        );
    }

    public PatchProposal completeRepair() {
        PatchProposal implementation =
                implementation();

        PatchProposal tests = tests();

        List<ProposedFileChange> changes =
                new ArrayList<>();

        changes.addAll(implementation.changes());
        changes.addAll(tests.changes());

        return new PatchProposal(
                "Repair and restore the complete analytics change",
                changes,
                implementation.assumptions(),
                implementation.risks()
        );
    }

    private ProposedFileChange source(
            String fileName,
            String templateName,
            String rationale
    ) {
        return new ProposedFileChange(
                FileChangeType.CREATE,
                "src/main/java/com/prasad/fixture/" +
                        "url_shortener/analytics/" +
                        fileName,
                null,
                template(templateName),
                rationale
        );
    }

    private String template(String name) {
        ClassPathResource resource =
                new ClassPathResource(
                        TEMPLATE_ROOT + name
                );

        try {
            return resource.getContentAsString(
                    StandardCharsets.UTF_8
            );
        } catch (IOException exception) {
            throw new ModelInvocationException(
                    "Unable to load deterministic template: " +
                            name,
                    exception
            );
        }
    }
}
