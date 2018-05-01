/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package com.atlassian.httpclient.apache.httpcomponents.cache;

import org.apache.http.client.cache.HttpCacheEntry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Copied from {@link org.apache.http.impl.client.cache.CacheMap} v4.1.2
 */
final class CacheMap extends LinkedHashMap<String, HttpCacheEntry> {

    private static final long serialVersionUID = -7750025207539768511L;

    private final int maxEntries;

    CacheMap(int maxEntries) {
        super(20, 0.75f, true);
        this.maxEntries = maxEntries;
    }

    @Override
    protected boolean removeEldestEntry(final Map.Entry<String, HttpCacheEntry> eldest) {
        return size() > this.maxEntries;
    }

}