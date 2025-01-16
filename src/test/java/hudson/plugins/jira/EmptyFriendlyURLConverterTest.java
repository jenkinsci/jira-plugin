package hudson.plugins.jira;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import java.net.URL;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

/**
 * @author lanwen (Merkushev Kirill)
 */
@WithJenkins
public class EmptyFriendlyURLConverterTest {

    public static final String SOME_VALID_URL = "http://localhost/";

    @Test
    @WithoutJenkins
    void shouldHandleURLClass() throws Exception {
        URL someUrl = new URL(SOME_VALID_URL);
        assertThat(new EmptyFriendlyURLConverter().convert(URL.class, someUrl), Matchers.is(someUrl));
    }

    @Test
    @WithoutJenkins
    void shouldHandleStringClass() throws Exception {
        assertThat(
                new EmptyFriendlyURLConverter().convert(URL.class, SOME_VALID_URL),
                Matchers.is(new URL(SOME_VALID_URL)));
    }

    @Test
    @WithoutJenkins
    void shouldHandleNull() throws Exception {
        assertThat(new EmptyFriendlyURLConverter().convert(URL.class, null), nullValue());
    }

    @Test
    @WithoutJenkins
    void shouldHandleEmptyString() throws Exception {
        assertThat(new EmptyFriendlyURLConverter().convert(URL.class, StringUtils.EMPTY), nullValue());
    }

    @Test
    @WithoutJenkins
    void shouldHandleNullAsString() throws Exception {
        assertThat(new EmptyFriendlyURLConverter().convert(URL.class, "null"), nullValue());
    }

    /**
     * Requires jenkins rule because of LOGGER usage starts descriptor creating
     */
    @Test
    void shouldHandleMalformedUrlAsString(JenkinsRule j) throws Exception {
        assertThat(new EmptyFriendlyURLConverter().convert(URL.class, "bla"), nullValue());
    }
}
