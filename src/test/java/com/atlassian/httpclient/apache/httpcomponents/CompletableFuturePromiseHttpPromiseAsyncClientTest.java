package com.atlassian.httpclient.apache.httpcomponents;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.atlassian.sal.api.executor.ThreadLocalContextManager;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.protocol.HttpContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class CompletableFuturePromiseHttpPromiseAsyncClientTest {

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
    private CompletableFuturePromiseHttpPromiseAsyncClient<Object> asyncClient;

    @Test
    void ensureCloseHttpclientOnCompletion() throws IOException {
        when(client.execute(eq(request), eq(context), any())).then((Answer<Future<HttpResponse>>) invocation -> {
            invocation.getArgument(2, FutureCallback.class).completed(response);
            return mock(Future.class);
        });

        asyncClient.execute(request, context);

        verify(client).close();
    }

    @Test
    void ensureCloseHttpclientOnFailure() throws IOException {
        when(client.execute(eq(request), eq(context), any())).then((Answer<Future<HttpResponse>>) invocation -> {
            invocation.getArgument(2, FutureCallback.class).failed(null);
            return mock(Future.class);
        });

        asyncClient.execute(request, context);

        verify(client).close();
    }

    @Test
    void ensureCloseHttpclientOnCancellation() throws IOException {
        when(client.execute(eq(request), eq(context), any())).then((Answer<Future<HttpResponse>>) invocation -> {
            invocation.getArgument(2, FutureCallback.class).cancelled();
            return mock(Future.class);
        });

        asyncClient.execute(request, context);

        verify(client).close();
    }
}
