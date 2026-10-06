package com.example.search.crdt;

import org.graalvm.polyglot.HostAccess;

import java.security.SecureRandom;
import java.util.Base64;

public final class YjsHostBridge {
    private final SecureRandom random = new SecureRandom();

    @HostAccess.Export
    public String randomBase64(int length) {
        if (length < 0 || length > 65_536) {
            throw new IllegalArgumentException("Invalid random byte length");
        }
        byte[] bytes = new byte[length];
        random.nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
