package hudson.plugins.jira.wiremock;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;

/**
 * Checks that a WireMock fixture body actually conforms to Atlassian's official Jira Cloud
 * platform OpenAPI spec, instead of trusting that by hand-inspection alone. Complements, but
 * doesn't replace, the manual cross-checking already done when a fixture is written: this
 * catches divergence from the documented contract (wrong types, missing required fields), not
 * client-library-specific parsing quirks that lie outside the spec itself.
 */
final class OpenApiSpecConformance {

    private static final String SPEC_URL = "https://developer.atlassian.com/cloud/jira/platform/swagger-v3.v3.json";

    private static final OpenApiInteractionValidator VALIDATOR =
            OpenApiInteractionValidator.createFor(SPEC_URL).build();

    private OpenApiSpecConformance() {}

    /**
     * Asserts that {@code jsonBody} conforms to the spec's schema for the given operation.
     *
     * @param specPath the spec's path template, e.g. {@code "/rest/api/3/issue/{issueIdOrKey}"}
     *     (the spec only documents {@code v3} paths; fixtures served under
     *     {@code /rest/api/2}/{@code /rest/api/latest} have the same wire shape, so validate
     *     against the equivalent {@code v3} path regardless of which path the fixture is
     *     actually stubbed on)
     */
    static void assertConformsToSpec(String specPath, Request.Method method, int status, String jsonBody) {
        SimpleResponse response = SimpleResponse.Builder.status(status)
                .withContentType("application/json")
                .withBody(jsonBody)
                .build();

        ValidationReport report = VALIDATOR.validateResponse(specPath, method, response);

        if (report.hasErrors()) {
            String messages = report.getMessages().stream()
                    .map(ValidationReport.Message::getMessage)
                    .collect(Collectors.joining("\n  - ", "  - ", ""));
            Assertions.fail("Fixture for " + method + " " + specPath
                    + " does not conform to the Jira Cloud OpenAPI spec:\n" + messages);
        }
    }
}
