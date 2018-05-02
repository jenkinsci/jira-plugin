package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.httpclient.api.Common;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

public class CommonBuilder<T> implements Common<CommonBuilder<T>>
{
    private final Headers.Builder headersBuilder = new Headers.Builder();
    private InputStream entityStream;

    @Override
    public CommonBuilder<T> setHeader(final String name, final String value)
    {
        headersBuilder.setHeader(name, value);
        return this;
    }

    @Override
    public CommonBuilder<T> setHeaders(final Map<String, String> headers)
    {
        headersBuilder.setHeaders(headers);
        return this;
    }

    @Override
    public CommonBuilder<T> setEntity(final String entity)
    {
        if (entity != null)
        {
            final String charset = "UTF-8";
            byte[] bytes = entity.getBytes(Charset.forName(charset));
            setEntityStream(new EntityByteArrayInputStream(bytes), charset);
        }
        else
        {
            setEntityStream(null, null);
        }
        return this;
    }

    @Override
    public CommonBuilder<T> setEntityStream(final InputStream entityStream)
    {
        this.entityStream = entityStream;
        return this;
    }

    @Override
    public CommonBuilder<T> setContentCharset(final String contentCharset)
    {
        headersBuilder.setContentCharset(contentCharset);
        return this;
    }

    @Override
    public CommonBuilder<T> setContentType(final String contentType)
    {
        headersBuilder.setContentType(contentType);
        return this;
    }

    @Override
    public CommonBuilder<T> setEntityStream(final InputStream entityStream, final String charset)
    {
        setEntityStream(entityStream);
        headersBuilder.setContentCharset(charset);
        return this;
    }

    public InputStream getEntityStream()
    {
        return entityStream;
    }

    public Headers getHeaders()
    {
        return headersBuilder.build();
    }

}
