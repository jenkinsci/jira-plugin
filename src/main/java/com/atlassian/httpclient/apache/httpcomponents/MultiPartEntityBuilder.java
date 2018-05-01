package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.httpclient.api.EntityBuilder;
import com.google.common.collect.Maps;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Builder for HttpEntities with multipart/form data.
 */
public class MultiPartEntityBuilder implements EntityBuilder
{
    private final HttpEntity apacheMultipartEntity;

    /**
     * @deprecated since 0.22. Use {@link #MultiPartEntityBuilder(org.apache.http.HttpEntity)} instead.
     */
    @Deprecated
    public MultiPartEntityBuilder(final MultipartEntity multipartEntity)
    {
        this.apacheMultipartEntity = multipartEntity;
    }

    /**
     * @since 0.22
     */
    public MultiPartEntityBuilder(final HttpEntity multipartEntity)
    {
        this.apacheMultipartEntity = multipartEntity;
    }

    private static class MultiPartEntity implements Entity
    {
        private final Map<String, String> headers;
        private final InputStream inputStream;

        public MultiPartEntity(Map<String, String> headers, InputStream inputStream)
        {
            this.headers = headers;
            this.inputStream = inputStream;
        }

        @Override
        public Map<String, String> getHeaders()
        {
            return this.headers;
        }

        @Override
        public InputStream getInputStream()
        {
            return this.inputStream;
        }
    }

    @Override
    public Entity build()
    {
        try
        {
            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            apacheMultipartEntity.writeTo(outputStream);
            final InputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
            final Header header = apacheMultipartEntity.getContentType();
            final Map<String, String> headers = Maps.newHashMap();
            headers.put(header.getName(), header.getValue());
            return new MultiPartEntity(headers, inputStream);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
