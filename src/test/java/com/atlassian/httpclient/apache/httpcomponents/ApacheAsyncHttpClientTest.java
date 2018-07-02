package com.atlassian.httpclient.apache.httpcomponents;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.httpclient.api.Response;
import com.atlassian.httpclient.api.factory.HttpClientOptions;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.UrlMode;
import com.atlassian.sal.api.executor.ThreadLocalContextManager;
import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.B64Code;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ApacheAsyncHttpClientTest
{

    @Rule
    public JenkinsRule j = new JenkinsRule();

    private final ConnectionFactory connectionFactory = new HttpConnectionFactory();

    private Server server;

    private ServerConnector connector;


    static final String CONTENT_RESPONSE = "Sounds Good";


    public void prepare( Handler handler )
        throws Exception
    {
        server = new Server();
        connector = new ServerConnector( server, connectionFactory );
        server.addConnector( connector );
        server.setHandler( handler );
        server.start();
    }

    @After
    public void dispose()
        throws Exception
    {
        if ( server != null )
        {
            server.stop();
        }
    }

    @Test
    public void simple_get()
        throws Exception
    {
        TestHandler testHandler = new TestHandler();
        prepare( testHandler );

        ApacheAsyncHttpClient httpClient =
            new ApacheAsyncHttpClient( (EventPublisher) null, buildApplicationProperties(),
                                       new NoOpThreadLocalContextManager(), new HttpClientOptions() );

        Response response = httpClient.newRequest( "http://localhost:" + connector.getLocalPort() + "/foo" ) //
            .get().get( 10, TimeUnit.SECONDS );
        Assert.assertEquals( 200, response.getStatusCode() );
        Assert.assertEquals( CONTENT_RESPONSE, IOUtils.toString( response.getEntityStream() ) );
    }

    @Test
    public void simple_post()
        throws Exception
    {
        TestHandler testHandler = new TestHandler();
        prepare( testHandler );

        ApacheAsyncHttpClient httpClient =
            new ApacheAsyncHttpClient( (EventPublisher) null, buildApplicationProperties(),
                                       new NoOpThreadLocalContextManager(), new HttpClientOptions() );

        Response response = httpClient.newRequest( "http://localhost:" + connector.getLocalPort() + "/foo" ) //
            .setEntity( "FOO" ) //
            .setContentType( "text" ) //
            .post().get( 10, TimeUnit.SECONDS );
        Assert.assertEquals( 200, response.getStatusCode() );
        Assert.assertEquals( CONTENT_RESPONSE, IOUtils.toString( response.getEntityStream() ) );
        Assert.assertEquals( "FOO", testHandler.postReceived );
    }

    @Test
    public void simple_get_with_non_proxy_host()
            throws Exception
    {
        ProxyTestHandler testHandler = new ProxyTestHandler();
        prepare( testHandler );

        Jenkins.getInstance().proxy = new ProxyConfiguration( "localhost", connector.getLocalPort(), "foo", "bar", "www.apache.org" );

        ApacheAsyncHttpClient httpClient =
                new ApacheAsyncHttpClient( (EventPublisher) null, buildApplicationProperties(),
                        new NoOpThreadLocalContextManager(), new HttpClientOptions() );

        Response response = httpClient.newRequest( "http://www.apache.org" )
                .get().get( 30, TimeUnit.SECONDS );
        Assert.assertEquals( 200, response.getStatusCode() );
        //Assert.assertEquals( CONTENT_RESPONSE, IOUtils.toString( response.getEntityStream() ) );
    }

    @Test
    public void simple_get_with_proxy()
        throws Exception
    {
        ProxyTestHandler testHandler = new ProxyTestHandler();
        prepare( testHandler );

        Jenkins.getInstance().proxy = new ProxyConfiguration( "localhost", connector.getLocalPort(), "foo", "bar" );

        ApacheAsyncHttpClient httpClient =
            new ApacheAsyncHttpClient( (EventPublisher) null, buildApplicationProperties(),
                                       new NoOpThreadLocalContextManager(), new HttpClientOptions() );

        Response response = httpClient.newRequest( "http://jenkins.io" ) //
            .get().get( 30, TimeUnit.SECONDS );
        Assert.assertEquals( 200, response.getStatusCode() );
        Assert.assertEquals( CONTENT_RESPONSE, IOUtils.toString( response.getEntityStream() ) );
    }

    @Test
    public void simple_post_with_proxy()
        throws Exception
    {
        ProxyTestHandler testHandler = new ProxyTestHandler();
        prepare( testHandler );

        Jenkins.getInstance().proxy = new ProxyConfiguration( "localhost", connector.getLocalPort(), "foo", "bar" );

        ApacheAsyncHttpClient httpClient =
            new ApacheAsyncHttpClient( (EventPublisher) null, buildApplicationProperties(),
                                       new NoOpThreadLocalContextManager(), new HttpClientOptions() );

        Response response = httpClient.newRequest( "http://jenkins.io" ) //
            .setEntity( "FOO" ) //
            .setContentType( "text" ) //
            .post().get( 30, TimeUnit.SECONDS );
        // we are sure to hit the proxy first :-)
        Assert.assertEquals( 200, response.getStatusCode() );
        Assert.assertEquals( CONTENT_RESPONSE, IOUtils.toString( response.getEntityStream() ) );
        Assert.assertEquals( "FOO", testHandler.postReceived );
    }


    public class ProxyTestHandler
        extends AbstractHandler
    {

        String postReceived;

        final String user = "foo";

        final String password = "bar";

        final String credentials = B64Code.encode( user + ":" + password, StandardCharsets.ISO_8859_1 );

        final String serverHost = "server";

        final String realm = "test_realm";

        @Override
        public void handle( String target, org.eclipse.jetty.server.Request jettyRequest, HttpServletRequest request,
                            HttpServletResponse response )
            throws IOException, ServletException
        {
            jettyRequest.setHandled( true );

            String authorization = request.getHeader( HttpHeader.PROXY_AUTHORIZATION.asString() );
            if ( authorization == null )
            {
                response.setStatus( HttpStatus.PROXY_AUTHENTICATION_REQUIRED_407 );
                response.setHeader( HttpHeader.PROXY_AUTHENTICATE.asString(), "Basic realm=\"" + realm + "\"" );
                return;
            }
            else
            {
                String prefix = "Basic ";
                if ( authorization.startsWith( prefix ) )
                {
                    String attempt = authorization.substring( prefix.length() );
                    if ( !credentials.equals( attempt ) )
                    {
                        return;
                    }
                }
            }

            if ( StringUtils.equalsIgnoreCase( "post", request.getMethod() ) )
            {
                postReceived = IOUtils.toString( request.getReader() );
            }
            response.getWriter().write( CONTENT_RESPONSE );

        }
    }


    public class TestHandler
        extends AbstractHandler
    {

        String postReceived;

        @Override
        public void handle( String target, org.eclipse.jetty.server.Request jettyRequest, HttpServletRequest request,
                            HttpServletResponse response )
            throws IOException, ServletException
        {
            jettyRequest.setHandled( true );
            if ( StringUtils.equalsIgnoreCase( "post", request.getMethod() ) )
            {
                postReceived = IOUtils.toString( request.getReader() );
            }
            response.getWriter().write( CONTENT_RESPONSE );

        }
    }

    private ApplicationProperties buildApplicationProperties()
    {
        ApplicationProperties applicationProperties = new ApplicationProperties()
        {
            @Override
            public String getBaseUrl()
            {
                return null;
            }

            @Nonnull
            @Override
            public String getBaseUrl( UrlMode urlMode )
            {
                return null;
            }

            @Nonnull
            @Override
            public String getDisplayName()
            {
                return "Foo";
            }

            @Nonnull
            @Override
            public String getPlatformId()
            {
                return null;
            }

            @Nonnull
            @Override
            public String getVersion()
            {
                return "1";
            }

            @Nonnull
            @Override
            public Date getBuildDate()
            {
                return null;
            }

            @Nonnull
            @Override
            public String getBuildNumber()
            {
                return "1";
            }

            @Nullable
            @Override
            public File getHomeDirectory()
            {
                return null;
            }

            @Override
            public String getPropertyValue( String s )
            {
                return null;
            }
        };
        return applicationProperties;
    }


    private static final class NoOpThreadLocalContextManager<C>
        implements ThreadLocalContextManager<C>
    {
        @Override
        public C getThreadLocalContext()
        {
            return null;
        }

        @Override
        public void setThreadLocalContext( C context )
        {
        }

        @Override
        public void clearThreadLocalContext()
        {
        }
    }

}
