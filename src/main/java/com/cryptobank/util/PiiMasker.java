package com.cryptobank.util;

/**
 * Enterprise PII (Personally Identifiable Information) Sanitizer.
 * In compliance with banking data privacy standards (PCI-DSS, GDPR, RBI Master Directions),
 * customer accounts, real names, and contact details must be masked before sending
 * context prompts to external Large Language Model (LLM) APIs.
 */
public final class PiiMasker {

    private PiiMasker() {}

    /**
     * Masks an 8-digit account number, preserving only the last 4 digits (e.g. 10004921 -> ****4921).
     */
    public static String maskAccount(Long accountNumber) {
        if (accountNumber == null) return "****0000";
        String str = String.valueOf(accountNumber);
        if (str.length() <= 4) return "****" + str;
        return "****" + str.substring(str.length() - 4);
    }

    /**
     * Masks a full name for privacy (e.g. "Suraj Kumar" -> "S**** K****").
     */
    public static String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Customer";
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.length() <= 1) {
                sb.append(p);
            } else {
                sb.append(p.charAt(0)).append("****");
            }
            if (i < parts.length - 1) sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Masks email address (e.g. "demo@bank.app" -> "d***@bank.app").
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "c***@bank.app";
        int at = email.indexOf('@');
        String name = email.substring(0, at);
        String domain = email.substring(at);
        if (name.length() <= 1) return name + "***" + domain;
        return name.charAt(0) + "***" + domain;
    }
}
