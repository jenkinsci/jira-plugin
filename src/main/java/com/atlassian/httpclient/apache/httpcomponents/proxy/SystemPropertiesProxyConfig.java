package com.atlassian.httpclient.apache.httpcomponents.proxy;

import com.atlassian.fugue.Option;
import com.atlassian.fugue.Options;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.NTCredentials;
import org.apache.http.auth.UsernamePasswordCredentials;

import java.util.List;

/**
 * Proxy configuration set with system properties.
 */
public class SystemPropertiesProxyConfig extends ProxyConfig
{
    private static final Iterable<String> SUPPORTED_SCHEMAS = Lists.newArrayList("http", "https");
    private static Splitter NON_PROXY_HOST_SPLITTER = Splitter.on('|');

    Iterable<HttpHost> getProxyHosts()
    {
        Iterable<Option<HttpHost>> proxyHosts = Iterables.transform(SUPPORTED_SCHEMAS, new Function<String, Option<HttpHost>>()
        {
            @Override
            public Option<HttpHost> apply(final String schema)
            {
                return getProxy(schema);
            }
        });
        return Options.flatten(Options.filterNone(proxyHosts));
    }

    @Override
    public Iterable<AuthenticationInfo> getAuthenticationInfo()
    {
        return Iterables.transform(getProxyHosts(), new Function<HttpHost, AuthenticationInfo>()
        {
            @Override
            public AuthenticationInfo apply(final HttpHost httpHost)
            {
                final AuthScope authScope = new AuthScope(httpHost);
                final Option<Credentials> credentials = credentialsForScheme(httpHost.getSchemeName());
                return new AuthenticationInfo(authScope, credentials);
            }
        });
    }

    private static Option<HttpHost> getProxy(final String schemeName)
    {
        String proxyHost = System.getProperty(schemeName + ".proxyHost");
        if (proxyHost != null)
        {
            return Option.some(new HttpHost(proxyHost, Integer.parseInt(System.getProperty(schemeName + ".proxyPort")), schemeName));
        }
        else
        {
            return Option.none();
        }
    }

    private static List<String> getNonProxyHosts(final String schemeName)
    {
        String nonProxyHosts = System.getProperty(schemeName + ".nonProxyHosts");
        if (nonProxyHosts != null)
        {
            return Lists.newArrayList(NON_PROXY_HOST_SPLITTER.split(nonProxyHosts));
        }
        else
        {
            return ImmutableList.of();
        }
    }

    private static Option<Credentials> credentialsForScheme(final String schemeName)
    {
        final String username = System.getProperty(schemeName + ".proxyUser");
        if (username != null)
        {
            final String proxyPassword = System.getProperty(schemeName + ".proxyPassword");
            final String proxyAuth = System.getProperty(schemeName + ".proxyAuth");
            if (proxyAuth == null || proxyAuth.equalsIgnoreCase("basic"))
            {
                return Option.<Credentials>some(new UsernamePasswordCredentials(username, proxyPassword));
            }
            else if (proxyAuth.equalsIgnoreCase("digest") || proxyAuth.equalsIgnoreCase("ntlm"))
            {
                String ntlmDomain = System.getProperty(schemeName + ".proxyNtlmDomain");
                String ntlmWorkstation = System.getProperty(schemeName + ".proxyNtlmWorkstation");
                return Option.<Credentials>some(new NTCredentials(username, proxyPassword,
                        StringUtils.defaultString(ntlmWorkstation), StringUtils.defaultString(ntlmDomain)));
            }
            else
            {
                return Option.none();
            }
        }
        else
        {
            return Option.none();
        }
    }

}
