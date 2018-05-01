package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.fugue.Effect;
import com.atlassian.httpclient.api.Request;
import com.google.common.io.ByteStreams;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class RequestEntityEffect implements Effect<HttpRequestBase>
{
    private final Request request;

    public RequestEntityEffect(final Request request)
    {
        this.request = request;
    }

    @Override
    public void apply(final HttpRequestBase httpRequestBase)
    {
        if (httpRequestBase instanceof HttpEntityEnclosingRequestBase)
        {
            ((HttpEntityEnclosingRequestBase) httpRequestBase).setEntity(getHttpEntity(request));
        }
        else
        {
            throw new UnsupportedOperationException("HTTP method " + request.getMethod() + " does not support sending an entity");
        }
    }

    private HttpEntity getHttpEntity(final Request request)
    {
        HttpEntity entity = null;
        if (request.hasEntity())
        {
            InputStream entityStream = request.getEntityStream();
            if (entityStream instanceof ByteArrayInputStream)
            {
                byte[] bytes;
                if (entityStream instanceof EntityByteArrayInputStream)
                {
                    bytes = ((EntityByteArrayInputStream) entityStream).getBytes();
                }
                else
                {
                    try
                    {
                        bytes = ByteStreams.toByteArray(entityStream);
                    }
                    catch (IOException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                entity = new ByteArrayEntity(bytes);
            }
            else
            {
                long contentLength = request.getContentLength().getOrElse(-1L);
                entity = new InputStreamEntity(entityStream, contentLength);
            }
        }
        return entity;
    }
}
