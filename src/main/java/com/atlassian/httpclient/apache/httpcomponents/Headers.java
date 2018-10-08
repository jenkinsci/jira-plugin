package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.httpclient.api.Buildable;
import com.google.common.base.Preconditions;
import org.apache.http.protocol.HTTP;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Headers
{
    private final Map<String, String> headers;
    private final String contentCharset;
    private final String contentType;

    private Headers(Map<String, String> headers, String contentCharset, String contentType)
    {
        this.headers = headers;
        this.contentCharset = contentCharset;
        this.contentType = contentType;
    }

    public String getContentCharset()
    {
        return contentCharset;
    }

    public String getContentType()
    {
        return contentType;
    }

    public Map<String, String> getHeaders()
    {
        Map<String, String> headers = new HashMap( this.headers);
        if (contentType != null)
        {
            headers.put(Names.CONTENT_TYPE, buildContentType());
        }
        return Collections.unmodifiableMap(headers);
    }

    public String getHeader(final String name)
    {
        String value;
        if (name.equalsIgnoreCase(Names.CONTENT_TYPE))
        {
            value = buildContentType();
        }
        else
        {
            value = headers.get(name);
        }
        return value;
    }

    private String buildContentType()
    {
        String value = contentType != null ? contentType : "text/plain";
        if (contentCharset != null)
        {
            value += "; charset=" + contentCharset;
        }
        return value;
    }

    public static class Builder implements Buildable<Headers>
    {
        private final Map<String, String> headers = new HashMap();
        private String contentType;
        private String contentCharset;

        public Builder setHeaders(Map<String, String> headers)
        {
            this.headers.clear();
            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                setHeader(entry.getKey(), entry.getValue());
            }
            return this;
        }

        public Builder setHeader(String name, String value)
        {
            if (name.equalsIgnoreCase("Content-Type"))
            {
                parseContentType(value);
            }
            else
            {
                headers.put(name, value);
            }
            return this;
        }

        public Builder setContentLength(long contentLength)
        {
            Preconditions.checkArgument(contentLength >= 0, "Content-Length must be greater than or equal to 0");
            setHeader(Names.CONTENT_LENGTH, Long.toString(contentLength));
            return this;
        }

        public Builder setContentCharset(String contentCharset)
        {
            this.contentCharset = contentCharset != null ? Charset.forName(contentCharset).name() : null;
            return this;
        }

        public Builder setContentType(String contentType)
        {
            parseContentType(contentType);
            return this;
        }

        private void parseContentType(String value)
        {
            if (value != null)
            {
                String[] parts = value.split(";");
                if (parts.length >= 1)
                {
                    contentType = parts[0].trim();
                }
                if (parts.length >= 2)
                {
                    String subtype = parts[1].trim();
                    if (subtype.startsWith("charset="))
                    {
                        setContentCharset(subtype.substring(8));
                    }
                    else if (subtype.startsWith("boundary="))
                    {
                        contentType = contentType.concat(';' + subtype);
                    }
                }
            }
            else
            {
                contentType = null;
            }
        }

        @Override
        public Headers build()
        {
            return new Headers(headers, contentCharset, contentType);
        }
    }

    public static class Names
    {
        public static final String CONTENT_LENGTH = HTTP.CONTENT_LEN;
        public static final String CONTENT_TYPE = HTTP.CONTENT_TYPE;

        private Names() {}
    }
}
