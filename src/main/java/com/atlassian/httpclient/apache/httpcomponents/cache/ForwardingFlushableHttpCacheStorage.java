package com.atlassian.httpclient.apache.httpcomponents.cache;

import java.util.regex.Pattern;

public abstract class ForwardingFlushableHttpCacheStorage extends ForwardingHttpCacheStorage implements FlushableHttpCacheStorage
{
    @Override
    protected abstract FlushableHttpCacheStorage delegate();

    @Override
    public void flushByUriPattern(Pattern urlPattern)
    {
        delegate().flushByUriPattern(urlPattern);
    }
}
