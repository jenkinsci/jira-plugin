package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.fugue.Option;
import com.atlassian.httpclient.api.Message;
import org.apache.http.util.CharArrayBuffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * An abstract base class for HTTP messages (i.e. Request and Response) with support for
 * header and entity management.
 */
abstract class DefaultMessage implements Message
{
    private final InputStream entityStream;
    private final Headers headers;
    private final long maxEntitySize;
    private boolean hasRead;

    public DefaultMessage(final Headers headers, final InputStream entityStream, Option<Long> maxEntitySize)
    {
        this.maxEntitySize = maxEntitySize.getOrElse((long) Integer.MAX_VALUE);
        this.headers = headers;
        this.entityStream = entityStream;
    }

    public String getContentType()
    {
        return headers.getContentType();
    }

    public String getContentCharset()
    {
        return headers.getContentCharset();
    }

    public String getAccept()
    {
        return headers.getHeader("Accept");
    }

    public InputStream getEntityStream() throws IllegalStateException
    {
        checkRead();
        return entityStream;
    }

    public String getEntity() throws IllegalStateException, IllegalArgumentException
    {
        String entity = null;
        if (hasEntity())
        {
            checkValidSize();
            final String charsetAsString = getContentCharset();
            final Charset charset = charsetAsString != null ? Charset.forName(charsetAsString) : Charset.forName(
                    "UTF-8");
            try
            {
                InputStream instream = getEntityStream();
                if (instream == null)
                {
                    return null;
                }
                try
                {
                    int bufferLength = 4096;
                    String lengthHeader = getHeader("Content-Length");
                    if (lengthHeader != null)
                    {
                        bufferLength = Integer.parseInt(lengthHeader);
                    }

                    Reader reader = new InputStreamReader(instream, charset);
                    CharArrayBuffer buffer = new CharArrayBuffer(bufferLength);
                    char[] tmp = new char[1024];
                    int l;
                    while ((l = reader.read(tmp)) != -1)
                    {
                        if (buffer.length() + l > maxEntitySize)
                        {
                            throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
                        }
                        buffer.append(tmp, 0, l);
                    }
                    return buffer.toString();
                }
                finally
                {
                    instream.close();
                }
            }
            catch (IOException e)
            {
                throw new IllegalStateException("Unable to convert response body to String", e);
            }
        }
        return entity;
    }

    public boolean hasEntity()
    {
        return entityStream != null;
    }

    public boolean hasReadEntity()
    {
        return hasRead;
    }

    public Map<String, String> getHeaders()
    {
        return headers.getHeaders();
    }

    public String getHeader(String name)
    {
        return headers.getHeader(name);
    }

    public Message validate()
    {
        if (hasEntity() && headers.getContentType() == null)
        {
            throw new IllegalStateException("Property contentType must be set when entity is present");
        }
        return this;
    }

    private void checkRead() throws IllegalStateException
    {
        if (entityStream != null)
        {
            if (hasRead)
            {
                throw new IllegalStateException("Entity may only be accessed once");
            }
            hasRead = true;
        }
    }

    private void checkValidSize() throws IllegalArgumentException
    {
        Integer contentLength;
        String lengthHeader = getHeader("Content-Length");
        if (lengthHeader != null)
        {
            contentLength = Integer.parseInt(lengthHeader);
            if (contentLength > maxEntitySize)
            {
                throw new IllegalArgumentException("HTTP entity too large to be buffered in memory");
            }
        }
    }
}
