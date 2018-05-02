package com.atlassian.httpclient.base.event;

import java.util.Map;

abstract class AbstractHttpRequestEvent
{
    private final String url;
    private final String httpMethod;
    private final long requestDuration;
    private final Map<String, String> properties;

    private int statusCode;
    private String error;

    public AbstractHttpRequestEvent(String url, String httpMethod, int statusCode, long requestDuration, Map<String, String> properties)
    {
        this.url = url;
        this.httpMethod = httpMethod;
        this.statusCode = statusCode;
        this.requestDuration = requestDuration;
        this.properties = properties;
    }

    public AbstractHttpRequestEvent(String url, String httpMethod, String error, long requestDuration, Map<String, String> properties)
    {
        this.url = url;
        this.httpMethod = httpMethod;
        this.error = error;
        this.requestDuration = requestDuration;
        this.properties = properties;
    }

    public String getUrl()
    {
        return url;
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public String getError()
    {
        return error;
    }

    public long getRequestDuration()
    {
        return requestDuration;
    }

    public Map<String, String> getProperties()
    {
        return properties;
    }
}
