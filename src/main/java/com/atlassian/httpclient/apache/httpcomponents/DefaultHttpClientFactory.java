package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.httpclient.api.HttpClient;
import com.atlassian.httpclient.api.factory.HttpClientFactory;
import com.atlassian.httpclient.api.factory.HttpClientOptions;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.executor.ThreadLocalContextManager;
import org.springframework.beans.factory.DisposableBean;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Objects;

public final class DefaultHttpClientFactory implements HttpClientFactory, DisposableBean
{
    private final EventPublisher eventPublisher;
    private final ApplicationProperties applicationProperties;
    private final ThreadLocalContextManager threadLocalContextManager;
    // shared http client
    private static ApacheAsyncHttpClient httpClient;

    public DefaultHttpClientFactory(EventPublisher eventPublisher, ApplicationProperties applicationProperties, ThreadLocalContextManager threadLocalContextManager)
    {
        this.eventPublisher = Objects.requireNonNull(eventPublisher);
        this.applicationProperties = Objects.requireNonNull(applicationProperties);
        this.threadLocalContextManager = Objects.requireNonNull(threadLocalContextManager);
    }

    @Override
    public HttpClient create(HttpClientOptions options)
    {
        return doCreate(options, threadLocalContextManager);
    }

    @Override
    public HttpClient create(HttpClientOptions options, ThreadLocalContextManager threadLocalContextManager)
    {
        return doCreate(options, threadLocalContextManager);
    }

    @Override
    public void dispose(@NonNull final HttpClient httpClient) throws Exception
    {
        if (httpClient instanceof ApacheAsyncHttpClient)
        {
            ((ApacheAsyncHttpClient) httpClient).destroy();
        }
    }

    private HttpClient doCreate(HttpClientOptions options, ThreadLocalContextManager threadLocalContextManager)
    {
        Objects.requireNonNull(options);
        // we create only one http client instance as we don't need more

        if(httpClient!=null) {
            return httpClient;
        }
        synchronized ( this )
        {
            httpClient =
                new ApacheAsyncHttpClient( eventPublisher, applicationProperties, threadLocalContextManager, options );
            return httpClient;
        }
    }

    @Override
    public void destroy() throws Exception
    {
        httpClient.destroy();
    }
}
