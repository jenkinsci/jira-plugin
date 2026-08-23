package com.atlassian.httpclient.apache.httpcomponents;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.httpclient.api.HttpClient;
import com.atlassian.httpclient.api.factory.HttpClientOptions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class DefaultHttpClientFactoryTest {

    private final List<HttpClient> created = new ArrayList<>();

    @AfterEach
    void destroyClients() {
        for (HttpClient client : created) {
            try {
                ((ApacheAsyncHttpClient) client).destroy();
            } catch (Exception e) {
                // best effort - a client disposed by the test itself is already shut down
            }
        }
        created.clear();
    }

    @Test
    void eachFactoryBuildsItsOwnClient(JenkinsRule r) {
        HttpClient first = create(newFactory(), optionsWithSocketTimeout(7));
        HttpClient second = create(newFactory(), optionsWithSocketTimeout(42));

        // The cache used to be static, so the second site silently ran on the first site's options.
        assertNotSame(first, second);
    }

    @Test
    void oneFactoryReusesItsOwnClient(JenkinsRule r) {
        DefaultHttpClientFactory factory = newFactory();

        assertSame(create(factory, optionsWithSocketTimeout(7)), create(factory, optionsWithSocketTimeout(7)));
    }

    @Test
    void disposeMakesTheFactoryBuildAFreshClient(JenkinsRule r) throws Exception {
        DefaultHttpClientFactory factory = newFactory();
        HttpClient disposed = create(factory, optionsWithSocketTimeout(7));

        factory.dispose(disposed);

        // Without this the factory kept handing out a client whose executor was already shut down.
        assertNotSame(disposed, create(factory, optionsWithSocketTimeout(7)));
    }

    @Test
    void destroyBeforeAnyClientWasCreatedDoesNotThrow(JenkinsRule r) {
        DefaultHttpClientFactory factory = newFactory();

        // destroy() used to dereference the cached client unconditionally.
        assertDoesNotThrow(factory::destroy);
    }

    private HttpClient create(DefaultHttpClientFactory factory, HttpClientOptions options) {
        HttpClient client = factory.create(options);
        created.add(client);
        return client;
    }

    private static DefaultHttpClientFactory newFactory() {
        return new DefaultHttpClientFactory(
                new NoOpEventPublisher(),
                ApacheAsyncHttpClientTest.buildApplicationProperties(),
                new ApacheAsyncHttpClientTest.NoOpThreadLocalContextManager<>());
    }

    private static HttpClientOptions optionsWithSocketTimeout(int seconds) {
        HttpClientOptions options = new HttpClientOptions();
        options.setSocketTimeout(seconds, TimeUnit.SECONDS);
        return options;
    }

    private static final class NoOpEventPublisher implements EventPublisher {
        @Override
        public void publish(Object o) {
            // no-op: these tests don't exercise event publishing
        }

        @Override
        public void register(Object o) {
            // no-op: these tests don't exercise event publishing
        }

        @Override
        public void unregister(Object o) {
            // no-op: these tests don't exercise event publishing
        }

        @Override
        public void unregisterAll() {
            // no-op: these tests don't exercise event publishing
        }
    }
}
