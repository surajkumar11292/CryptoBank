package com.cryptobank.util;

import java.util.UUID;

public final class ReferenceGenerator {

    private ReferenceGenerator() {}

    public static String generateTransactionReference() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
