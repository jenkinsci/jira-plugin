package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.sal.api.executor.ThreadLocalContextManager;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.SettableFuture;
import io.atlassian.util.concurrent.Promise;
import io.atlassian.util.concurrent.Promises;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

final class SettableFuturePromiseHttpPromiseAsyncClient<C> implements PromiseHttpAsyncClient {

  private final Logger log = LoggerFactory.getLogger(this.getClass());

  private final CloseableHttpAsyncClient client;
  private final ThreadLocalContextManager<C> threadLocalContextManager;
  private final Executor executor;

  SettableFuturePromiseHttpPromiseAsyncClient(CloseableHttpAsyncClient client,
      ThreadLocalContextManager<C> threadLocalContextManager, Executor executor) {
    this.client = checkNotNull(client);
    this.threadLocalContextManager = checkNotNull(threadLocalContextManager);
    this.executor = new ThreadLocalDelegateExecutor<C>(threadLocalContextManager, executor);
  }

  @VisibleForTesting
  static <C> void runInContext(ThreadLocalContextManager<C> threadLocalContextManager,
      C threadLocalContext, ClassLoader contextClassLoader, Runnable runnable) {
    final C oldThreadLocalContext = threadLocalContextManager.getThreadLocalContext();
    final ClassLoader oldCcl = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(contextClassLoader);
      threadLocalContextManager.setThreadLocalContext(threadLocalContext);
      runnable.run();
    } finally {
      threadLocalContextManager.setThreadLocalContext(oldThreadLocalContext);
      Thread.currentThread().setContextClassLoader(oldCcl);
    }
  }

  @Override
  public Promise<HttpResponse> execute(HttpUriRequest request, HttpContext context) {
    // TODO after migrating from atlassian-util-concurrent 3.0.0 to 4.0.0 the SettableFuture.create() maybe obsolete ?
    final SettableFuture<HttpResponse> future = SettableFuture.create();
    Future<org.apache.http.HttpResponse> clientFuture = client.execute(request, context,
        new ThreadLocalContextAwareFutureCallback<C>(threadLocalContextManager) {
          @Override
          void doCompleted(final HttpResponse httpResponse) {
            executor.execute(() -> future.set(httpResponse));
            log.trace("Closing in doCompleted()");
            closeClient();
          }

          @Override
          void doFailed(final Exception ex) {
            executor.execute(() -> future.setException(ex));
            log.trace("Closing in doFailed()");
            closeClient();
          }

          @Override
          void doCancelled() {
            final TimeoutException timeoutException = new TimeoutException();
            executor.execute(() -> future.setException(timeoutException));
            log.trace("Closing in doCancelled()");
            closeClient();
          }
        });
    return Promises.forFuture(clientFuture, executor);
  }

  private void closeClient() {
    try {
      client.close();
    } catch (IOException e) {
      log.error("Close failed", e);
    }
  }

  private static abstract class ThreadLocalContextAwareFutureCallback<C> implements
      FutureCallback<HttpResponse> {

    private final ThreadLocalContextManager<C> threadLocalContextManager;
    private final C threadLocalContext;
    private final ClassLoader contextClassLoader;

    private ThreadLocalContextAwareFutureCallback(
        ThreadLocalContextManager<C> threadLocalContextManager) {
      this.threadLocalContextManager = checkNotNull(threadLocalContextManager);
      this.threadLocalContext = threadLocalContextManager.getThreadLocalContext();
      this.contextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    abstract void doCompleted(HttpResponse response);

    abstract void doFailed(Exception ex);

    abstract void doCancelled();

    @Override
    public final void completed(final HttpResponse response) {
      runInContext(threadLocalContextManager, threadLocalContext, contextClassLoader,
          () -> doCompleted(response));
    }

    @Override
    public final void failed(final Exception ex) {
      runInContext(threadLocalContextManager, threadLocalContext, contextClassLoader,
          () -> doFailed(ex));
    }

    @Override
    public final void cancelled() {
      runInContext(threadLocalContextManager, threadLocalContext, contextClassLoader,
          () -> doCancelled());
    }
  }

  private static final class ThreadLocalDelegateExecutor<C> implements Executor {

    private final Executor delegate;
    private final ThreadLocalContextManager<C> manager;

    ThreadLocalDelegateExecutor(ThreadLocalContextManager<C> manager, Executor delegate) {
      this.delegate = checkNotNull(delegate);
      this.manager = checkNotNull(manager);
    }

    public void execute(final Runnable runnable) {
      delegate.execute(new ThreadLocalDelegateRunnable<C>(manager, runnable));
    }
  }

  private static final class ThreadLocalDelegateRunnable<C> implements Runnable {

    private final C context;
    private final Runnable delegate;
    private final ClassLoader contextClassLoader;
    private final ThreadLocalContextManager<C> manager;

    ThreadLocalDelegateRunnable(ThreadLocalContextManager<C> manager, Runnable delegate) {
      this.delegate = delegate;
      this.manager = manager;
      this.context = manager.getThreadLocalContext();
      this.contextClassLoader = Thread.currentThread().getContextClassLoader();
    }

    public void run() {
      runInContext(manager, context, contextClassLoader, () -> delegate.run());
    }
  }
}
