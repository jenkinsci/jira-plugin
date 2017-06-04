package hudson.plugins.jira;

import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.WithoutJenkins;

import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author lanwen (Merkushev Kirill)
 */
public class EmptyFriendlyURLConverterTest {

    public static final String SOME_VALID_URL = "http://localhost/";
    
    @Rule
    public JenkinsRule jRule = new JenkinsRule();

    @Test
    @WithoutJenkins
    public void shouldHandleURLClass() throws Exception {
        URL someUrl = new URL(SOME_VALID_URL);
        assertThat(new EmptyFriendlyURLConverter()
                .convert(URL.class, someUrl), Matchers.<Object>is(someUrl));
    }

    @Test
    @WithoutJenkins
    public void shouldHandleStringClass() throws Exception {
        assertThat(new EmptyFriendlyURLConverter()
                .convert(URL.class, SOME_VALID_URL), Matchers.<Object>is(new URL(SOME_VALID_URL)));
    }

    @Test
    @WithoutJenkins
    public void shouldHandleNull() throws Exception {
        assertThat(new EmptyFriendlyURLConverter()
                .convert(URL.class, null), nullValue());
    }

    @Test
    @WithoutJenkins
    public void shouldHandleEmptyString() throws Exception {
        assertThat(new EmptyFriendlyURLConverter()
                .convert(URL.class, StringUtils.EMPTY), nullValue());
    }

    @Test
    @WithoutJenkins
    public void shouldHandleNullAsString() throws Exception {
        assertThat(new EmptyFriendlyURLConverter()
                .convert(URL.class, "null"), nullValue());
    }

    /**
     * Requires jenkins rule because of LOGGER usage starts descriptor creating
     */
    @Test
    public void shouldHandleMalformedUrlAsString() throws Exception {
        assertThat(new EmptyFriendlyURLConverter()
                .convert(URL.class, "bla"), nullValue());
    }

}
