package com.atlassian.httpclient.base.event;

import java.util.Map;

public final class HttpRequestFailedEvent extends AbstractHttpRequestEvent
{
    public HttpRequestFailedEvent(String url, String httpMethod, int statusCode, long elapsed, Map<String, String> properties)
    {
        super(url, httpMethod, statusCode, elapsed, properties);
    }

    public HttpRequestFailedEvent(String url, String httpMethod, String error, long elapsed, Map<String, String> properties)
    {
        super(url, httpMethod, error, elapsed, properties);
    }
}
