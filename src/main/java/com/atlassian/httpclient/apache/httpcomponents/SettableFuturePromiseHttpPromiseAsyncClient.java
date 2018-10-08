package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.sal.api.executor.ThreadLocalContextManager;
import com.atlassian.util.concurrent.Promise;
import com.atlassian.util.concurrent.Promises;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.SettableFuture;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.protocol.HttpContext;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;

import static com.google.common.base.Preconditions.checkNotNull;

final class SettableFuturePromiseHttpPromiseAsyncClient<C> implements PromiseHttpAsyncClient
{
    private final HttpAsyncClient client;
    private final ThreadLocalContextManager<C> threadLocalContextManager;
    private final Executor executor;

    SettableFuturePromiseHttpPromiseAsyncClient(HttpAsyncClient client, ThreadLocalContextManager<C> threadLocalContextManager, Executor executor)
    {
        this.client = checkNotNull(client);
        this.threadLocalContextManager = checkNotNull(threadLocalContextManager);
        this.executor = new ThreadLocalDelegateExecutor<C>(threadLocalContextManager, executor);
    }

    @Override
    public Promise<HttpResponse> execute(HttpUriRequest request, HttpContext context)
    {
        final SettableFuture<HttpResponse> future = SettableFuture.create();
        client.execute(request, context, new ThreadLocalContextAwareFutureCallback<C, HttpResponse>(threadLocalContextManager)
        {
            @Override
            void doCompleted(final HttpResponse httpResponse)
            {
                executor.execute( () -> future.set(httpResponse));
            }

            @Override
            void doFailed(final Exception ex)
            {
                executor.execute(() -> future.setException(ex));
            }

            @Override
            void doCancelled()
            {
                final TimeoutException timeoutException = new TimeoutException();
                executor.execute(() -> future.setException(timeoutException));
            }
        });
        return Promises.forListenableFuture(future);
    }

    @VisibleForTesting
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

    private static abstract class ThreadLocalContextAwareFutureCallback<C, HttpResponse> implements FutureCallback<HttpResponse>
    {
        private final ThreadLocalContextManager<C> threadLocalContextManager;
        private final C threadLocalContext;
        private final ClassLoader contextClassLoader;

        private ThreadLocalContextAwareFutureCallback(ThreadLocalContextManager<C> threadLocalContextManager)
        {
            this.threadLocalContextManager = checkNotNull(threadLocalContextManager);
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
            runInContext(threadLocalContextManager, threadLocalContext, contextClassLoader, () -> doCancelled());
        }
    }

    private static final class ThreadLocalDelegateExecutor<C> implements Executor
    {
        private final Executor delegate;
        private final ThreadLocalContextManager<C> manager;

        ThreadLocalDelegateExecutor(ThreadLocalContextManager<C> manager, Executor delegate)
        {
            this.delegate = checkNotNull(delegate);
            this.manager = checkNotNull(manager);
        }

        public void execute(final Runnable runnable)
        {
            delegate.execute(new ThreadLocalDelegateRunnable<C>(manager, runnable));
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
            runInContext(manager, context, contextClassLoader,  () -> delegate.run());
        }
    }
}
