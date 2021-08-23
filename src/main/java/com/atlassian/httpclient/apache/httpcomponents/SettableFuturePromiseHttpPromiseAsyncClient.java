package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.sal.api.executor.ThreadLocalContextManager;
import io.atlassian.util.concurrent.Promise;
import io.atlassian.util.concurrent.Promises;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;


final class SettableFuturePromiseHttpPromiseAsyncClient<C> implements PromiseHttpAsyncClient
{
    private final Logger log = LoggerFactory.getLogger( this.getClass() );

    private final CloseableHttpAsyncClient client;
    private final ThreadLocalContextManager<C> threadLocalContextManager;
    private final Executor executor;

    SettableFuturePromiseHttpPromiseAsyncClient(CloseableHttpAsyncClient client, ThreadLocalContextManager<C> threadLocalContextManager, Executor executor)
    {
        this.client = Objects.requireNonNull(client);
        this.threadLocalContextManager = Objects.requireNonNull(threadLocalContextManager);
        this.executor = new ThreadLocalDelegateExecutor<>(threadLocalContextManager, executor);
    }

    @Override
    public Promise<HttpResponse> execute(HttpUriRequest request, HttpContext context)
    {
    	// TODO after migrating from atlassian-util-concurrent 3.0.0 to 4.0.0 the SettableFuture.create() maybe obsolete ?
        Future<org.apache.http.HttpResponse> clientFuture = client.execute(request, context, new ThreadLocalContextAwareFutureCallback<C>(threadLocalContextManager)
        {

            @Override
            void doCompleted(final HttpResponse httpResponse)
            {
                log.trace( "Closing in doCompleted()" );
                closeClient();
            }

            @Override
            void doFailed(final Exception ex)
            {
                executor.execute(() -> {throw new RuntimeException(ex);});
                log.trace( "Closing in doFailed()" );
                closeClient();
            }

            @Override
            void doCancelled()
            {
                final TimeoutException timeoutException = new TimeoutException();
                executor.execute(() -> {throw new RuntimeException(timeoutException);});
                log.trace( "Closing in doCancelled()" );
                closeClient();
            }
        });
        return Promises.forFuture(clientFuture,executor);
    }

    private void closeClient() {
        try {
            client.close();
        } catch ( IOException e ) {
            log.error( "Close failed" , e );
        }
    }

    static <C> void runInContext(ThreadLocalContextManager<C> threadLocalContextManager, C threadLocalContext, ClassLoader contextClassLoader, Runnable runnable)
    {
        final C oldThreadLocalContext = threadLocalContextManager.getThreadLocalContext();
        final ClassLoader oldCcl = Thread.currentThread().getContextClassLoader();
        try
        {
            Thread.currentThread().setContextClassLoader(contextClassLoader);
            threadLocalContextManager.setThreadLocalContext(threadLocalContext);
            runnable.run();
        }
        finally
        {
            threadLocalContextManager.setThreadLocalContext(oldThreadLocalContext);
            Thread.currentThread().setContextClassLoader(oldCcl);
        }
    }

    private static abstract class ThreadLocalContextAwareFutureCallback<C> implements FutureCallback<HttpResponse>
    {
        private final ThreadLocalContextManager<C> threadLocalContextManager;
        private final C threadLocalContext;
        private final ClassLoader contextClassLoader;

        private ThreadLocalContextAwareFutureCallback(ThreadLocalContextManager<C> threadLocalContextManager)
        {
            this.threadLocalContextManager = Objects.requireNonNull(threadLocalContextManager);
            this.threadLocalContext = threadLocalContextManager.getThreadLocalContext();
            this.contextClassLoader = Thread.currentThread().getContextClassLoader();
        }

        abstract void doCompleted(HttpResponse response);

        abstract void doFailed(Exception ex);

        abstract void doCancelled();

        @Override
        public final void completed(final HttpResponse response)
        {
            runInContext(threadLocalContextManager, threadLocalContext, contextClassLoader, () -> doCompleted(response));
        }

        @Override
        public final void failed(final Exception ex)
        {
            runInContext(threadLocalContextManager, threadLocalContext, contextClassLoader, () ->  doFailed(ex));
        }

        @Override
        public final void cancelled()
        {
            runInContext(threadLocalContextManager, threadLocalContext, contextClassLoader, this::doCancelled);
        }
    }

    private static final class ThreadLocalDelegateExecutor<C> implements Executor
    {
        private final Executor delegate;
        private final ThreadLocalContextManager<C> manager;

        ThreadLocalDelegateExecutor(ThreadLocalContextManager<C> manager, Executor delegate)
        {
            this.delegate = Objects.requireNonNull(delegate);
            this.manager = Objects.requireNonNull(manager);
        }

        public void execute(Runnable runnable)
        {
            delegate.execute(new ThreadLocalDelegateRunnable<>(manager, runnable));
        }
    }

    private static final class ThreadLocalDelegateRunnable<C> implements Runnable
    {
        private final C context;
        private final Runnable delegate;
        private final ClassLoader contextClassLoader;
        private final ThreadLocalContextManager<C> manager;

        ThreadLocalDelegateRunnable(ThreadLocalContextManager<C> manager, Runnable delegate)
        {
            this.delegate = delegate;
            this.manager = manager;
            this.context = manager.getThreadLocalContext();
            this.contextClassLoader = Thread.currentThread().getContextClassLoader();
        }

        public void run()
        {
            runInContext(manager, context, contextClassLoader, delegate);
        }
    }
}
