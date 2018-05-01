package com.atlassian.httpclient.base.event;

import java.util.Map;


public final class HttpRequestCompletedEvent extends AbstractHttpRequestEvent
{
    public HttpRequestCompletedEvent(String url, String httpMethod, int statusCode, long requestDuration, Map<String, String> properties)
    {
        super(url, httpMethod, statusCode, requestDuration, properties);
    }
}
