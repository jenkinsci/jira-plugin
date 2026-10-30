// SPDX-License-Identifier: Apache-2.0
// See ADR 0003 (docs/adr/0003-future-of-the-vendored-atlassian-http-client.md)

package com.atlassian.httpclient.apache.httpcomponents;

import io.atlassian.util.concurrent.Promise;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;

interface PromiseHttpAsyncClient {
    Promise<HttpResponse> execute(HttpUriRequest request, HttpContext context);
}
