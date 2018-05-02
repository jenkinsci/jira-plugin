package com.atlassian.httpclient.apache.httpcomponents.proxy;

import com.atlassian.fugue.Option;
import com.atlassian.httpclient.api.factory.HttpClientOptions;
import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import org.apache.http.HttpHost;

import static com.atlassian.httpclient.apache.httpcomponents.proxy.ProxyConfig.AuthenticationInfo;

public class ProxyConfigFactory
{
    public static Option<HttpHost> getProxyHost(final HttpClientOptions options)
    {
        return getProxyConfig(options).fold(new Supplier<Option<HttpHost>>()
        {
            @Override
            public Option<HttpHost> get()
            {
                return Option.none();
            }
        }, new Function<ProxyConfig, Option<HttpHost>>()
        {
            @Override
            public Option<HttpHost> apply(final ProxyConfig proxyConfig)
            {
                return proxyConfig.getProxyHost();
            }
        });
    }

    public static Iterable<AuthenticationInfo> getProxyAuthentication(final HttpClientOptions options)
    {
        return getProxyConfig(options).fold(new Supplier<Iterable<AuthenticationInfo>>()
        {
            @Override
            public Iterable<AuthenticationInfo> get()
            {
                return Lists.newLinkedList();
            }
        }, new Function<ProxyConfig, Iterable<AuthenticationInfo>>()
        {
            @Override
            public Iterable<AuthenticationInfo> apply(final ProxyConfig proxyConfig)
            {
                return proxyConfig.getAuthenticationInfo();
            }
        });
    }

    private static Option<ProxyConfig> getProxyConfig(final HttpClientOptions options)
    {
        final Option<ProxyConfig> config;
        switch (options.getProxyOptions().getProxyMode())
        {
            case SYSTEM_PROPERTIES:
                config = Option.<ProxyConfig>some(new SystemPropertiesProxyConfig());
                break;
            case CONFIGURED:
                config = Option.<ProxyConfig>some(new ProvidedProxyConfig(options.getProxyOptions().getProxyHosts(), options.getProxyOptions().getNonProxyHosts()));
                break;
            default:
                config = Option.none();
        }
        return config;
    }
}
