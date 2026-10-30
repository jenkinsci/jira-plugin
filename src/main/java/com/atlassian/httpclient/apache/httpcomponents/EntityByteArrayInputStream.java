// SPDX-License-Identifier: Apache-2.0
// See ADR 0003 (docs/adr/0003-future-of-the-vendored-atlassian-http-client.md)

package com.atlassian.httpclient.apache.httpcomponents;

import java.io.ByteArrayInputStream;

public class EntityByteArrayInputStream extends ByteArrayInputStream {
    private byte[] bytes;

    public EntityByteArrayInputStream(byte[] bytes) {
        super(bytes);
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return bytes;
    }
}
