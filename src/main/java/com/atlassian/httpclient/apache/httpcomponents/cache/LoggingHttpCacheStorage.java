package com.atlassian.httpclient.apache.httpcomponents.cache;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import org.apache.http.client.cache.HttpCacheEntry;
import org.apache.http.client.cache.HttpCacheUpdateCallback;
import org.apache.http.client.cache.HttpCacheUpdateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;

public final class LoggingHttpCacheStorage extends ForwardingFlushableHttpCacheStorage
{
    private final Logger logger;

    private final FlushableHttpCacheStorage httpCacheStorage;
    private final Supplier<String> instanceId;

    public LoggingHttpCacheStorage(FlushableHttpCacheStorage httpCacheStorage)
    {
        this.httpCacheStorage = checkNotNull(httpCacheStorage);
        this.instanceId = Suppliers.memoize(new Supplier<String>()
        {
            @Override
            public String get()
            {
                return Integer.toHexString(System.identityHashCode(LoggingHttpCacheStorage.this));
            }
        });
        this.logger = LoggerFactory.getLogger(delegate().getClass());
    }

    @Override
    protected FlushableHttpCacheStorage delegate()
    {
        return httpCacheStorage;
    }

    @Override
    public void flushByUriPattern(Pattern urlPattern)
    {
        logger.debug("Cache [{}] is flushing entries matching {}", instanceId.get(), urlPattern);
        super.flushByUriPattern(urlPattern);
    }

    @Override
    public void putEntry(String key, HttpCacheEntry entry) throws IOException
    {
        logger.debug("Cache [{}] is adding '{}'s response: {}", new Object[]{instanceId.get(), key, toString(entry)});
        super.putEntry(key, entry);
    }

    @Override
    public HttpCacheEntry getEntry(String key) throws IOException
    {
        final HttpCacheEntry entry = super.getEntry(key);
        logger.debug("Cache [{}] is getting '{}'s response: {}", new Object[]{instanceId.get(), key, toString(entry)});
        return entry;
    }

    @Override
    public void removeEntry(String key) throws IOException
    {
        if (logger.isDebugEnabled())
        {
            logger.debug("Cache [{}] is removing '{}''s response: {}", new Object[]{instanceId.get(), key, toString(super.getEntry(key))});
        }
        super.removeEntry(key);
    }

    @Override
    public void updateEntry(String key, HttpCacheUpdateCallback callback) throws IOException, HttpCacheUpdateException
    {
        if (logger.isDebugEnabled())
        {
            final HttpCacheEntry oldEntry = super.getEntry(key);
            super.updateEntry(key, callback);
            final HttpCacheEntry newEntry = super.getEntry(key);
            logger.debug("Cache [{}] is updating '{}'s response from {} to {}", new Object[]{instanceId.get(), key, toString(oldEntry), toString(newEntry)});
        }
        else
        {
            super.updateEntry(key, callback);
        }
    }

    private static HttpCacheEntryToString toString(HttpCacheEntry httpCacheEntry)
    {
        return httpCacheEntry == null ? null : new HttpCacheEntryToString(httpCacheEntry);
    }

    private static final class HttpCacheEntryToString
    {
        private final HttpCacheEntry httpCacheEntry;

        private HttpCacheEntryToString(HttpCacheEntry httpCacheEntry)
        {
            this.httpCacheEntry = checkNotNull(httpCacheEntry);
        }

        @Override
        public String toString()
        {
            return httpCacheEntry.toString();
        }
    }
}
