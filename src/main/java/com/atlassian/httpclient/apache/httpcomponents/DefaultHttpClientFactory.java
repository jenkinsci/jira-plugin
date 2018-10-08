package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.httpclient.api.HttpClient;
import com.atlassian.httpclient.api.factory.HttpClientFactory;
import com.atlassian.httpclient.api.factory.HttpClientOptions;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.executor.ThreadLocalContextManager;
import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.Nonnull;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import static com.google.common.base.Preconditions.checkNotNull;

public final class DefaultHttpClientFactory implements HttpClientFactory, DisposableBean
{
    private final EventPublisher eventPublisher;
    private final ApplicationProperties applicationProperties;
    private final ThreadLocalContextManager threadLocalContextManager;
    private final Set<ApacheAsyncHttpClient> httpClients = new CopyOnWriteArraySet<ApacheAsyncHttpClient>();

    public DefaultHttpClientFactory(EventPublisher eventPublisher, ApplicationProperties applicationProperties, ThreadLocalContextManager threadLocalContextManager)
    {
        this.eventPublisher = checkNotNull(eventPublisher);
        this.applicationProperties = checkNotNull(applicationProperties);
        this.threadLocalContextManager = checkNotNull(threadLocalContextManager);
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
    public void dispose(@Nonnull final HttpClient httpClient) throws Exception
    {
        if (httpClient instanceof ApacheAsyncHttpClient)
        {
            final ApacheAsyncHttpClient client = (ApacheAsyncHttpClient) httpClient;
            if (httpClients.remove(client))
            {
                client.destroy();
            }
            else
            {
                throw new IllegalStateException("Client is already disposed");
            }
        }
        else
        {
            throw new IllegalArgumentException("Given client is not disposable");
        }
    }

    private HttpClient doCreate(HttpClientOptions options, ThreadLocalContextManager threadLocalContextManager)
    {
        checkNotNull(options);
        final ApacheAsyncHttpClient httpClient = new ApacheAsyncHttpClient(eventPublisher, applicationProperties, threadLocalContextManager, options);
        httpClients.add(httpClient);
        return httpClient;
    }

    @Override
    public void destroy() throws Exception
    {
        for (ApacheAsyncHttpClient httpClient : httpClients)
        {
            httpClient.destroy();
        }
    }

    @VisibleForTesting
    Iterable<ApacheAsyncHttpClient> getHttpClients()
    {
        return httpClients;
    }
}
