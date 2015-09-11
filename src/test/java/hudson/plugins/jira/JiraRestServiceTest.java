package hudson.plugins.jira;

import static org.junit.Assert.*;

import java.net.URI;

import org.junit.Test;

public class JiraRestServiceTest {

    @Test
    public void testBaseApiPath01() {
        URI uri = URI.create("http://example.com:8080/");
        JiraRestService service = new JiraRestService(uri, null, "user",
            "password");
        assertEquals("/" + JiraRestService.BASE_API_PATH,
            service.getBaseApiPath());
    }

    @Test
    public void testBaseApiPath02() {
        URI uri = URI.create("https://example.com/path/to/jira");
        JiraRestService service = new JiraRestService(uri, null, "user",
            "password");
        assertEquals("/path/to/jira/" + JiraRestService.BASE_API_PATH,
            service.getBaseApiPath());
    }
}
