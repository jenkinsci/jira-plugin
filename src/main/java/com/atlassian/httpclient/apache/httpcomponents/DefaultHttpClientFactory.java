package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.httpclient.api.HttpClient;
import com.atlassian.httpclient.api.factory.HttpClientFactory;
import com.atlassian.httpclient.api.factory.HttpClientOptions;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.executor.ThreadLocalContextManager;
import javax.annotation.Nonnull;
import org.springframework.beans.factory.DisposableBean;

import static com.google.common.base.Preconditions.checkNotNull;

public final class DefaultHttpClientFactory implements HttpClientFactory, DisposableBean {

  // shared http client
  private static ApacheAsyncHttpClient httpClient;
  private final EventPublisher eventPublisher;
  private final ApplicationProperties applicationProperties;
  private final ThreadLocalContextManager threadLocalContextManager;

  public DefaultHttpClientFactory(EventPublisher eventPublisher,
      ApplicationProperties applicationProperties,
      ThreadLocalContextManager threadLocalContextManager) {
    this.eventPublisher = checkNotNull(eventPublisher);
    this.applicationProperties = checkNotNull(applicationProperties);
    this.threadLocalContextManager = checkNotNull(threadLocalContextManager);
  }

  @Override
  public HttpClient create(HttpClientOptions options) {
    return doCreate(options, threadLocalContextManager);
  }

  @Override
  public HttpClient create(HttpClientOptions options,
      ThreadLocalContextManager threadLocalContextManager) {
    return doCreate(options, threadLocalContextManager);
  }

  @Override
  public void dispose(@Nonnull final HttpClient httpClient) throws Exception {
    if (httpClient instanceof ApacheAsyncHttpClient) {
      ((ApacheAsyncHttpClient) httpClient).destroy();
    }
  }

  private HttpClient doCreate(HttpClientOptions options,
      ThreadLocalContextManager threadLocalContextManager) {
    checkNotNull(options);
    // we create only one http client instance as we don't need more

    if (httpClient != null) {
      return httpClient;
    }
    synchronized (this) {
      httpClient =
          new ApacheAsyncHttpClient(eventPublisher, applicationProperties,
              threadLocalContextManager, options);
      return httpClient;
    }
  }

  @Override
  public void destroy() throws Exception {
    httpClient.destroy();
  }
}
