package com.atlassian.httpclient.apache.httpcomponents.proxy;


import com.atlassian.fugue.Option;
import com.google.common.collect.Iterables;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;

public abstract class ProxyConfig
{
    public Option<HttpHost> getProxyHost()
    {
        final HttpHost httpHost = Iterables.getFirst(getProxyHosts(), null);
        if (httpHost != null)
        {
            return Option.some(new HttpHost(httpHost.getHostName(), httpHost.getPort()));
        }
        else
        {
            return Option.none();
        }
    }

    abstract Iterable<HttpHost> getProxyHosts();

    public abstract Iterable<AuthenticationInfo> getAuthenticationInfo();

    public static class AuthenticationInfo
    {
        private final AuthScope authScope;
        private final Option<Credentials> credentials;

        public AuthenticationInfo(final AuthScope authScope, final Option<Credentials> credentials)
        {
            this.authScope = authScope;
            this.credentials = credentials;
        }

        public AuthScope getAuthScope()
        {
            return authScope;
        }

        public Option<Credentials> getCredentials()
        {
            return credentials;
        }
    }
}
