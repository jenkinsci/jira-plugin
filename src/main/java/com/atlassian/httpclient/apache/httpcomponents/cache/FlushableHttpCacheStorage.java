package com.atlassian.httpclient.apache.httpcomponents.cache;

import org.apache.http.client.cache.HttpCacheStorage;

import java.util.regex.Pattern;

public interface FlushableHttpCacheStorage extends HttpCacheStorage
{
    void flushByUriPattern(Pattern urlPattern);
}
