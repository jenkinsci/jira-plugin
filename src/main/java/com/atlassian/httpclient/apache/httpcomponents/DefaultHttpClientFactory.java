package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.httpclient.api.HttpClient;
import com.atlassian.httpclient.api.factory.HttpClientFactory;
import com.atlassian.httpclient.api.factory.HttpClientOptions;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.executor.ThreadLocalContextManager;
import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;
import org.springframework.beans.factory.DisposableBean;

public final class DefaultHttpClientFactory implements HttpClientFactory, DisposableBean {
    private final EventPublisher eventPublisher;
    private final ApplicationProperties applicationProperties;
    private final ThreadLocalContextManager threadLocalContextManager;

    /**
     * The client this factory has built, if any.
     *
     * <p>This is deliberately an <em>instance</em> field. It used to be {@code static}, which meant the
     * first Jira site to build a client pinned its {@link HttpClientOptions} — socket timeout, io thread
     * count, callback executor — and its application properties onto every other site in the JVM, until
     * Jenkins restarted. {@code JiraSite} builds one factory per client, so caching per instance keeps
     * the original "one client per site" intent without leaking one site's configuration into another.
     */
    private volatile ApacheAsyncHttpClient httpClient;

    public DefaultHttpClientFactory(
            EventPublisher eventPublisher,
            ApplicationProperties applicationProperties,
            ThreadLocalContextManager threadLocalContextManager) {
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.applicationProperties = Objects.requireNonNull(applicationProperties);
        this.threadLocalContextManager = Objects.requireNonNull(threadLocalContextManager);
    }

    @Override
    public HttpClient create(HttpClientOptions options) {
        return doCreate(options, threadLocalContextManager);
    }

    @Override
    public HttpClient create(HttpClientOptions options, ThreadLocalContextManager threadLocalContextManager) {
        return doCreate(options, threadLocalContextManager);
    }

    @Override
    public void dispose(@NonNull final HttpClient httpClient) throws Exception {
        if (httpClient instanceof ApacheAsyncHttpClient) {
            // Forget the client before destroying it, so a later create() builds a fresh one rather
            // than handing back an instance whose executor and connection manager are shut down.
            synchronized (this) {
                if (this.httpClient == httpClient) {
                    this.httpClient = null;
                }
            }
            ((ApacheAsyncHttpClient) httpClient).destroy();
        }
    }

    private HttpClient doCreate(HttpClientOptions options, ThreadLocalContextManager threadLocalContextManager) {
        Objects.requireNonNull(options);
        // we create only one http client instance per factory as we don't need more
        ApacheAsyncHttpClient existing = httpClient;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (httpClient == null) {
                httpClient = new ApacheAsyncHttpClient(
                        eventPublisher, applicationProperties, threadLocalContextManager, options);
            }
            return httpClient;
        }
    }

    @Override
    public void destroy() throws Exception {
        ApacheAsyncHttpClient client = httpClient;
        if (client != null) {
            client.destroy();
        }
    }
}
