package hudson.plugins.jira;

import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.assertEquals;

public class JiraRestServiceTest {

    @Test
    public void testBaseApiPath() {
        URI uri = URI.create("http://example.com:8080/");
        JiraRestService service = new JiraRestService(uri, null, "user", "password", JiraSite.DEFAULT_TIMEOUT);
        assertEquals("/" + JiraRestService.BASE_API_PATH, service.getBaseApiPath());

        uri = URI.create("https://example.com/path/to/jira");
        service = new JiraRestService(uri, null, "user", "password", JiraSite.DEFAULT_TIMEOUT);
        assertEquals("/path/to/jira/" + JiraRestService.BASE_API_PATH, service.getBaseApiPath());
    }

}
