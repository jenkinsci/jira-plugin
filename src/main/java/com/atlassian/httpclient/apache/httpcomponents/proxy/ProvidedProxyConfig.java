package com.atlassian.httpclient.apache.httpcomponents.proxy;

import com.atlassian.fugue.Option;
import com.atlassian.fugue.Options;
import com.atlassian.httpclient.api.factory.Host;
import com.atlassian.httpclient.api.factory.Scheme;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * HttpClientProxyConfig implementation that uses proxy configuration from construction, and not
 * from system properties.
 *
 * @since 0.20.0
 */
public class ProvidedProxyConfig extends ProxyConfig
{
    private static final Logger log = LoggerFactory.getLogger(ProvidedProxyConfig.class);
    private static final Iterable<String> SUPPORTED_SCHEMAS = Lists.newArrayList("http", "https");

    private final Map<String, HttpHost> proxyHostMap;
    private final Map<String, List<String>> nonProxyHosts;

    public ProvidedProxyConfig(@Nonnull final Map<Scheme, Host> proxyHostMap,
            @Nonnull final Map<Scheme, List<String>> nonProxyHosts)
    {
        Preconditions.checkNotNull(proxyHostMap);
        Preconditions.checkNotNull(nonProxyHosts);
        this.proxyHostMap = new HashMap<String, HttpHost>(proxyHostMap.size());
        for (Scheme s: proxyHostMap.keySet())
        {
            Host h = proxyHostMap.get(s);
            this.proxyHostMap.put(s.schemeName(), new HttpHost(h.getHost(), h.getPort()));
        }
        this.nonProxyHosts = new HashMap<>(nonProxyHosts.size());
        for (Scheme s: nonProxyHosts.keySet())
        {
            List<String> nonProxyHostList = nonProxyHosts.get(s);
            if (nonProxyHostList != null)
            {
                this.nonProxyHosts.put(s.schemeName(), ImmutableList.copyOf(nonProxyHostList));
            }
        }
    }

    Iterable<HttpHost> getProxyHosts()
    {
        final Iterable<Option<HttpHost>> httpHosts = Iterables.transform(SUPPORTED_SCHEMAS, new Function<String, Option<HttpHost>>()
        {
            @Override
            public Option<HttpHost> apply(final String schema)
            {
                return Option.option(proxyHostMap.get(schema));
            }
        });
        return Options.flatten(Options.filterNone(httpHosts));
    }

    @Override
    public Iterable<AuthenticationInfo> getAuthenticationInfo()
    {
        log.info("Authentication info not supported for ProvidedProxyConfig");
        return Collections.emptyList();
    }


}
