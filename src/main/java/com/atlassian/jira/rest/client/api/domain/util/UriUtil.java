/*
 * Copyright (C) 2014 Atlassian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* Temporary fix until https://bitbucket.org/atlassian/jira-rest-java-client/pull-requests/170 is fixed */

package com.atlassian.jira.rest.client.api.domain.util;

import java.net.URI;
import java.util.List;

public class UriUtil {

    private static final List<String> CLOUD_DOMAINS = List.of("atlassian.net", "jira.com", "api.atlassian.com");
    private static final List<String> DC_DOMAINS = List.of("localhost");

    public static URI path(final URI uri, final String path) {
        final String uriString = uri.toString();
        final StringBuilder sb = new StringBuilder(uriString);
        if (!uriString.endsWith("/")) {
            sb.append('/');
        }
        sb.append(path.startsWith("/") ? path.substring(1) : path);
        return URI.create(sb.toString());
    }

    public static boolean isURICloud(URI uri) {
        String host = uri.getHost().toLowerCase();
        return CLOUD_DOMAINS.stream().anyMatch(host::contains)
                && DC_DOMAINS.stream().noneMatch(host::contains);
    }
}
