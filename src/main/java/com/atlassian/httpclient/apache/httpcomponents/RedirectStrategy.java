package com.atlassian.httpclient.apache.httpcomponents;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.protocol.HttpContext;

import java.net.URI;

public class RedirectStrategy extends DefaultRedirectStrategy
{
    final String[] REDIRECT_METHODS = { HttpHead.METHOD_NAME, HttpGet.METHOD_NAME, HttpPost.METHOD_NAME, HttpPut.METHOD_NAME, HttpDelete.METHOD_NAME, HttpPatch.METHOD_NAME };

    @Override
    public boolean isRedirectable(String method)
    {
        for (String m : REDIRECT_METHODS)
        {
            if (m.equalsIgnoreCase(method))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public HttpUriRequest getRedirect(final HttpRequest request, final HttpResponse response, final HttpContext context)
            throws ProtocolException
    {
        URI uri = getLocationURI(request, response, context);
        String method = request.getRequestLine().getMethod();
        if (method.equalsIgnoreCase(HttpHead.METHOD_NAME))
        {
            return new HttpHead(uri);
        }
        else if (method.equalsIgnoreCase(HttpGet.METHOD_NAME))
        {
            return new HttpGet(uri);
        }
        else if (method.equalsIgnoreCase(HttpPost.METHOD_NAME))
        {
            final HttpPost post = new HttpPost(uri);
            if (request instanceof HttpEntityEnclosingRequest)
            {
                post.setEntity(((HttpEntityEnclosingRequest) request).getEntity());
            }
            return post;
        }
        else if (method.equalsIgnoreCase(HttpPut.METHOD_NAME))
        {
            return new HttpPut(uri);
        }
        else if (method.equalsIgnoreCase(HttpDelete.METHOD_NAME))
        {
            return new HttpDelete(uri);
        }
        else if (method.equalsIgnoreCase(HttpPatch.METHOD_NAME))
        {
            return new HttpPatch(uri);
        }
        else
        {
            return new HttpGet(uri);
        }
    }
}
