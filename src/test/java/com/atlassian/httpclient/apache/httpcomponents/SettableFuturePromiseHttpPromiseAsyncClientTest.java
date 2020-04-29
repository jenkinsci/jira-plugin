package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.sal.api.executor.ThreadLocalContextManager;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.protocol.HttpContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SettableFuturePromiseHttpPromiseAsyncClientTest {

  @Mock
  private CloseableHttpAsyncClient client;

  @Mock
  private ThreadLocalContextManager<Object> threadLocalContextManager;

  @Mock
  private Executor executor;

  @Mock
  private HttpUriRequest request;

  @Mock
  private HttpResponse response;

  @Mock
  private HttpContext context;

  @InjectMocks
  private SettableFuturePromiseHttpPromiseAsyncClient<Object> asyncClient;

  @Test
  public void ensureCloseHttpclientOnCompletion() throws IOException {
    when(client.execute(eq(request), eq(context), any()))
        .then(new Answer<Future<HttpResponse>>() {
          @Override
          public Future<HttpResponse> answer(InvocationOnMock invocation) throws Throwable {
            invocation.getArgumentAt(2, FutureCallback.class).completed(response);
            return mock(Future.class);
          }
        });

    asyncClient.execute(request, context);

    verify(client).close();
  }

  @Test
  public void ensureCloseHttpclientOnFailure() throws IOException {
    when(client.execute(eq(request), eq(context), any()))
        .then(new Answer<Future<HttpResponse>>() {
          @Override
          public Future<HttpResponse> answer(InvocationOnMock invocation) throws Throwable {
            invocation.getArgumentAt(2, FutureCallback.class).failed(null);
            return mock(Future.class);
          }
        });

    asyncClient.execute(request, context);

    verify(client).close();
  }

  @Test
  public void ensureCloseHttpclientOnCancellation() throws IOException {
    when(client.execute(eq(request), eq(context), any()))
        .then(new Answer<Future<HttpResponse>>() {
          @Override
          public Future<HttpResponse> answer(InvocationOnMock invocation) throws Throwable {
            invocation.getArgumentAt(2, FutureCallback.class).cancelled();
            return mock(Future.class);
          }
        });

    asyncClient.execute(request, context);

    verify(client).close();
  }
}
