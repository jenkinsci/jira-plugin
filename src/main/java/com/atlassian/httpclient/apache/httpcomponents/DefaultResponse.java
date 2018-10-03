package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.fugue.Option;
import com.atlassian.httpclient.api.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;

public final class DefaultResponse extends DefaultMessage implements Response
{
    private int statusCode;
    private String statusText;
    private Logger log = LoggerFactory.getLogger(DefaultResponse.class);

    public DefaultResponse(Headers headers, InputStream entityStream, Option<Long> maxEntitySize, int statusCode, String statusText)
    {
        super(headers, entityStream, maxEntitySize);
        this.statusCode = statusCode;
        this.statusText = statusText;
    }

    public static DefaultResponseBuilder builder()
    {
        return new DefaultResponseBuilder();
    }

    @Override
    public int getStatusCode()
    {
        return statusCode;
    }

    @Override
    public String getStatusText()
    {
        return statusText;
    }

    @Override
    public boolean isInformational()
    {
        return statusCode >= 100 && statusCode < 200;
    }

    @Override
    public boolean isSuccessful()
    {
        return statusCode >= 200 && statusCode < 300;
    }

    @Override
    public boolean isOk()
    {
        return statusCode == 200;
    }

    @Override
    public boolean isCreated()
    {
        return statusCode == 201;
    }

    @Override
    public boolean isNoContent()
    {
        return statusCode == 204;
    }

    @Override
    public boolean isRedirection()
    {
        return statusCode >= 300 && statusCode < 400;
    }

    @Override
    public boolean isSeeOther()
    {
        return statusCode == 303;
    }

    @Override
    public boolean isNotModified()
    {
        return statusCode == 304;
    }

    @Override
    public boolean isClientError()
    {
        return statusCode >= 400 && statusCode < 500;
    }

    @Override
    public boolean isBadRequest()
    {
        return statusCode == 400;
    }

    @Override
    public boolean isUnauthorized()
    {
        return statusCode == 401;
    }

    @Override
    public boolean isForbidden()
    {
        return statusCode == 403;
    }

    @Override
    public boolean isNotFound()
    {
        return statusCode == 404;
    }

    @Override
    public boolean isConflict()
    {
        return statusCode == 409;
    }

    @Override
    public boolean isServerError()
    {
        return statusCode >= 500 && statusCode < 600;
    }

    @Override
    public boolean isInternalServerError()
    {
        return statusCode == 500;
    }

    @Override
    public boolean isServiceUnavailable()
    {
        return statusCode == 503;
    }

    @Override
    public boolean isError()
    {
        return isClientError() || isServerError();
    }

    @Override
    public boolean isNotSuccessful()
    {
        return isInformational() || isRedirection() || isError();
    }

    @Override
    public Option<Long> getContentLength()
    {
        String lengthString = getHeader(Headers.Names.CONTENT_LENGTH);
        if (lengthString != null)
        {
            try
            {
                Option<Long> parsedLength = Option.some(Long.parseLong(lengthString));
                return parsedLength.flatMap( aLong -> {
                                if (aLong < 0)
                                {
                                    log.warn("Unable to parse content length. Received out of range value {}", aLong);
                                    return Option.none();
                                }
                                else
                                {
                                    return Option.some(aLong);
                                }
                        });
            }
            catch (NumberFormatException e)
            {
                log.warn("Unable to parse content length {}", lengthString);
                return Option.none();
            }
        }
        else
        {
            return Option.none();
        }
    }

    public static class DefaultResponseBuilder implements Builder
    {
        private final CommonBuilder<DefaultResponse> commonBuilder;

        private String statusText;
        private int statusCode;
        private long maxEntitySize;

        private DefaultResponseBuilder()
        {
            this.commonBuilder = new CommonBuilder<DefaultResponse>();
        }

        @Override
        public DefaultResponseBuilder setContentType(final String contentType)
        {
            commonBuilder.setContentType(contentType);
            return this;
        }

        @Override
        public DefaultResponseBuilder setContentCharset(final String contentCharset)
        {
            commonBuilder.setContentCharset(contentCharset);
            return this;
        }

        @Override
        public DefaultResponseBuilder setHeaders(final Map<String, String> headers)
        {
            commonBuilder.setHeaders(headers);
            return this;
        }

        @Override
        public DefaultResponseBuilder setHeader(final String name, final String value)
        {
            commonBuilder.setHeader(name, value);
            return this;
        }

        @Override
        public DefaultResponseBuilder setEntity(final String entity)
        {
            commonBuilder.setEntity(entity);
            return this;
        }

        @Override
        public DefaultResponseBuilder setEntityStream(final InputStream entityStream, final String encoding)
        {
            commonBuilder.setEntityStream(entityStream);
            commonBuilder.setContentCharset(encoding);
            return this;
        }

        @Override
        public DefaultResponseBuilder setEntityStream(final InputStream entityStream)
        {
            commonBuilder.setEntityStream(entityStream);
            return this;
        }

        @Override
        public DefaultResponseBuilder setStatusText(final String statusText)
        {
            this.statusText = statusText;
            return this;
        }

        @Override
        public DefaultResponseBuilder setStatusCode(final int statusCode)
        {
            this.statusCode = statusCode;
            return this;
        }

        public DefaultResponseBuilder setMaxEntitySize(long maxEntitySize)
        {
            this.maxEntitySize = maxEntitySize;
            return this;
        }

        @Override
        public DefaultResponse build()
        {
            return new DefaultResponse(commonBuilder.getHeaders(), commonBuilder.getEntityStream(),
                    Option.option(maxEntitySize), statusCode, statusText);
        }
    }
}
