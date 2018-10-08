package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.fugue.Option;
import com.atlassian.httpclient.api.EntityBuilder;
import com.atlassian.httpclient.api.HttpClient;
import com.atlassian.httpclient.api.Request;
import com.atlassian.httpclient.api.ResponsePromise;
import com.google.common.base.Preconditions;

import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.atlassian.httpclient.api.Request.Method.DELETE;
import static com.atlassian.httpclient.api.Request.Method.GET;
import static com.atlassian.httpclient.api.Request.Method.HEAD;
import static com.atlassian.httpclient.api.Request.Method.OPTIONS;
import static com.atlassian.httpclient.api.Request.Method.POST;
import static com.atlassian.httpclient.api.Request.Method.PUT;
import static com.atlassian.httpclient.api.Request.Method.TRACE;
import static com.google.common.base.Preconditions.checkNotNull;

public class DefaultRequest extends DefaultMessage implements Request
{
    private final URI uri;
    private final boolean cacheDisabled;
    private final Map<String, String> attributes;
    private final Method method;
    private final Option<Long> contentLength;

    private DefaultRequest(URI uri, boolean cacheDisabled, Map<String, String> attributes,
            Headers headers, Method method, InputStream entityStream, Option<Long> contentLength)
    {
        super(headers, entityStream, Option.<Long>none());
        this.uri = uri;
        this.cacheDisabled = cacheDisabled;
        this.attributes = attributes;
        this.method = method;
        this.contentLength = contentLength;
    }

    public static DefaultRequestBuilder builder(HttpClient httpClient)
    {
        return new DefaultRequestBuilder(httpClient);
    }

    @Override
    public Method getMethod()
    {
        return method;
    }

    @Override
    public URI getUri()
    {
        return uri;
    }

    @Override
    public String getAccept()
    {
        return super.getAccept();
    }

    @Override
    public String getAttribute(String name)
    {
        return attributes.get(name);
    }

    @Override
    public Map<String, String> getAttributes()
    {
        return Collections.unmodifiableMap(attributes);
    }

    @Override
    public Option<Long> getContentLength()
    {
        return contentLength;
    }

    public boolean isCacheDisabled()
    {
        return cacheDisabled;
    }

    public Request validate()
    {
        super.validate();

        checkNotNull(uri);
        checkNotNull(method);

        switch (method)
        {
            case GET:
            case DELETE:
            case HEAD:
                if (hasEntity())
                {
                    throw new IllegalStateException("Request method " + method + " does not support an entity");
                }
                break;
            case POST:
            case PUT:
            case TRACE:
                // no-op
                break;
        }
        return this;
    }

    public static class DefaultRequestBuilder implements Request.Builder
    {
        private final HttpClient httpClient;
        private final Map<String, String> attributes;
        private final CommonBuilder<DefaultRequest> commonBuilder;

        private URI uri;
        private boolean cacheDisabled;
        private Method method;
        private Option<Long> contentLength;

        public DefaultRequestBuilder(final HttpClient httpClient)
        {
            this.httpClient = httpClient;
            this.attributes = new HashMap<>();
            commonBuilder = new CommonBuilder<>();
            setAccept("*/*");
            contentLength = Option.none();
        }

        @Override
        public DefaultRequestBuilder setUri(final URI uri)
        {
            this.uri = uri;
            return this;
        }

        @Override
        public DefaultRequestBuilder setAccept(final String accept)
        {
            setHeader("Accept", accept);
            return this;
        }

        @Override
        public DefaultRequestBuilder setCacheDisabled()
        {
            this.cacheDisabled = true;
            return this;
        }

        @Override
        public DefaultRequestBuilder setAttribute(final String name, final String value)
        {
            attributes.put(name, value);
            return this;
        }

        @Override
        public DefaultRequestBuilder setAttributes(final Map<String, String> properties)
        {
            attributes.putAll(properties);
            return this;
        }

        @Override
        public DefaultRequestBuilder setEntity(final EntityBuilder entityBuilder)
        {
            EntityBuilder.Entity entity = entityBuilder.build();
            final Map<String, String> headers = entity.getHeaders();
            for (Map.Entry<String, String> headerEntry : headers.entrySet())
            {
                setHeader(headerEntry.getKey(), headerEntry.getValue());
            }
            setEntityStream(entity.getInputStream());
            return this;
        }

        @Override
        public DefaultRequestBuilder setHeader(final String name, final String value)
        {
            commonBuilder.setHeader(name, value);
            return this;
        }

        @Override
        public DefaultRequestBuilder setHeaders(final Map<String, String> headers)
        {
            commonBuilder.setHeaders(headers);
            return this;
        }

        @Override
        public DefaultRequestBuilder setEntity(final String entity)
        {
            commonBuilder.setEntity(entity);
            setContentLength(entity.length());
            return this;
        }

        @Override
        public DefaultRequestBuilder setEntityStream(final InputStream entityStream)
        {
            commonBuilder.setEntityStream(entityStream);
            return this;
        }

        @Override
        public DefaultRequestBuilder setContentCharset(final String contentCharset)
        {
            commonBuilder.setContentCharset(contentCharset);
            return this;
        }

        @Override
        public DefaultRequestBuilder setContentType(final String contentType)
        {
            commonBuilder.setContentType(contentType);
            return this;
        }

        @Override
        public DefaultRequestBuilder setEntityStream(final InputStream entityStream, final String charset)
        {
            setEntityStream(entityStream);
            commonBuilder.setContentCharset(charset);
            return this;
        }

        @Override
        public DefaultRequestBuilder setContentLength(final long contentLength)
        {
            Preconditions.checkArgument(contentLength >= 0, "Content length must be greater than or equal to 0");
            this.contentLength = Option.some(contentLength);
            return this;
        }

        @Override
        public DefaultRequest build()
        {
            return new DefaultRequest(uri, cacheDisabled, attributes, commonBuilder.getHeaders(),
                    method, commonBuilder.getEntityStream(), contentLength);
        }

        @Override
        public ResponsePromise get()
        {
            return execute(GET);
        }

        @Override
        public ResponsePromise post()
        {
            return execute(POST);
        }

        @Override
        public ResponsePromise put()
        {
            return execute(PUT);
        }

        @Override
        public ResponsePromise delete()
        {
            return execute(DELETE);
        }

        @Override
        public ResponsePromise options()
        {
            return execute(OPTIONS);
        }

        @Override
        public ResponsePromise head()
        {
            return execute(HEAD);
        }

        @Override
        public ResponsePromise trace()
        {
            return execute(TRACE);
        }

        @Override
        public ResponsePromise execute(Method method)
        {
            checkNotNull(method, "HTTP method must not be null");
            setMethod(method);
            return httpClient.execute(build().validate());
        }

        public void setMethod(final Method method)
        {
            this.method = method;
        }
    }
}
