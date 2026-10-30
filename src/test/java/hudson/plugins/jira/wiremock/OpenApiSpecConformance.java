package hudson.plugins.jira.wiremock;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.SimpleResponse;
import com.atlassian.oai.validator.report.ValidationReport;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;

/**
 * Checks that a WireMock fixture body actually conforms to Atlassian's official Jira Cloud
 * platform OpenAPI spec, instead of trusting that by hand-inspection alone. Complements, but
 * doesn't replace, the manual cross-checking already done when a fixture is written: this
 * catches divergence from the documented contract (wrong types, missing required fields), not
 * client-library-specific parsing quirks that lie outside the spec itself.
 *
 * <p>The spec is a trimmed copy checked into {@code src/test/resources}, not a download. Fetching
 * the 2.3 MB original in this class's static initialiser made the supposedly offline WireMock suite
 * depend on the network - and fail, offline, with a "cannot parse specification" error that never
 * mentioned it. Regenerate the copy with {@code node tools/trim-jira-openapi-spec.mjs}.
 */
final class OpenApiSpecConformance {

    private static final String SPEC_RESOURCE = "jira-cloud-platform-openapi-trimmed.json";

    private static final String SPEC_JSON = readSpec();

    private static final OpenApiInteractionValidator VALIDATOR =
            OpenApiInteractionValidator.createForInlineApiSpecification(SPEC_JSON)
                    .build();

    /**
     * The paths the trimmed copy actually carries. Validating against a path it does not contain
     * silently checks nothing, which would make trimming a way to lose coverage without noticing.
     */
    private static final Set<String> KNOWN_PATHS = pathsIn(SPEC_JSON);

    private OpenApiSpecConformance() {}

    private static String readSpec() {
        try (InputStream in = OpenApiSpecConformance.class.getResourceAsStream(SPEC_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                        SPEC_RESOURCE + " is missing - regenerate it with: node tools/trim-jira-openapi-spec.mjs");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("could not read " + SPEC_RESOURCE, e);
        }
    }

    private static Set<String> pathsIn(String specJson) {
        try {
            JsonNode paths = new ObjectMapper().readTree(specJson).path("paths");
            Set<String> names = new HashSet<>();
            paths.fieldNames().forEachRemaining(names::add);
            return names;
        } catch (IOException e) {
            throw new UncheckedIOException("could not parse " + SPEC_RESOURCE, e);
        }
    }

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
        if (!KNOWN_PATHS.contains(specPath)) {
            Assertions.fail("The trimmed OpenAPI spec does not describe " + specPath
                    + ", so this fixture would be validated against nothing. Add the path to KEPT_PATHS in"
                    + " tools/trim-jira-openapi-spec.mjs and re-run it.");
        }

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
