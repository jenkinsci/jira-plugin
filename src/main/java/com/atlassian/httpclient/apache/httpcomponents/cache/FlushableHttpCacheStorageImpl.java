package com.atlassian.httpclient.apache.httpcomponents.cache;

import org.apache.http.client.cache.HttpCacheEntry;
import org.apache.http.client.cache.HttpCacheUpdateCallback;
import org.apache.http.impl.client.cache.CacheConfig;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Originally copied from {@link org.apache.http.impl.client.cache.BasicHttpCacheStorage} v4.1.2
 *
 * Have added ability to flush cache
 */
public final class FlushableHttpCacheStorageImpl implements FlushableHttpCacheStorage
{
    private final CacheMap entries;

    public FlushableHttpCacheStorageImpl(CacheConfig config)
    {
        this.entries = new CacheMap(config.getMaxCacheEntries());
    }

    @Override
    public synchronized void flushByUriPattern(Pattern urlPattern)
    {
        for (Iterator<Map.Entry<String, HttpCacheEntry>> i = entries.entrySet().iterator(); i.hasNext(); )
        {
            final Map.Entry<String, HttpCacheEntry> entry = i.next();
            if (urlPattern.matcher(entry.getKey()).matches())
            {
                i.remove();
            }
        }
    }

    /**
     * Places a HttpCacheEntry in the cache
     *
     * @param url Url to use as the cache key
     * @param entry HttpCacheEntry to place in the cache
     */
    public synchronized void putEntry(String url, HttpCacheEntry entry) throws IOException
    {
        entries.put(url, entry);
    }

    /**
     * Gets an entry from the cache, if it exists
     *
     * @param url Url that is the cache key
     * @return HttpCacheEntry if one exists, or null for cache miss
     */
    public synchronized HttpCacheEntry getEntry(String url)
    {
        return entries.get(url);
    }

    /**
     * Removes a HttpCacheEntry from the cache
     *
     * @param url Url that is the cache key
     */
    public synchronized void removeEntry(String url) throws IOException
    {
        entries.remove(url);
    }

    public synchronized void updateEntry(String url, HttpCacheUpdateCallback callback) throws IOException
    {
        HttpCacheEntry existingEntry = entries.get(url);
        entries.put(url, callback.update(existingEntry));
    }
}
