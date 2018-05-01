package com.atlassian.httpclient.apache.httpcomponents.cache;

import com.google.common.collect.ForwardingObject;
import org.apache.http.client.cache.HttpCacheEntry;
import org.apache.http.client.cache.HttpCacheStorage;
import org.apache.http.client.cache.HttpCacheUpdateCallback;
import org.apache.http.client.cache.HttpCacheUpdateException;

import java.io.IOException;


public abstract class ForwardingHttpCacheStorage extends ForwardingObject implements HttpCacheStorage
{
    @Override
    protected abstract HttpCacheStorage delegate();

    @Override
    public void putEntry(String key, HttpCacheEntry entry) throws IOException
    {
        delegate().putEntry(key, entry);
    }

    @Override
    public HttpCacheEntry getEntry(String key) throws IOException
    {
        return delegate().getEntry(key);
    }

    @Override
    public void removeEntry(String key) throws IOException
    {
        delegate().removeEntry(key);
    }

    @Override
    public void updateEntry(String key, HttpCacheUpdateCallback callback) throws IOException, HttpCacheUpdateException
    {
        delegate().updateEntry(key, callback);
    }
}
