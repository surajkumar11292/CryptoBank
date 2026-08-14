package com.cryptobank.security;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Instant;

@Service
public class TotpService {

    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final int ALLOWED_DRIFT_STEPS = 1; // ±30s clock skew tolerance
    private static final String ISSUER = "CryptoBank";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    /** A fresh random 160-bit seed, Base32-encoded. */
    public String generateSecret() {
        byte[] bytes = new byte[20];
        RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /** otpauth:// URI for authenticator apps. */
    public String otpAuthUri(String secret, String accountEmail) {
        String label = ISSUER.replace(" ", "%20") + ":" + accountEmail;
        return "otpauth://totp/" + label
                + "?secret=" + secret
                + "&issuer=" + ISSUER.replace(" ", "%20")
                + "&digits=" + CODE_DIGITS
                + "&period=" + TIME_STEP_SECONDS;
    }

    /** True if the 6-digit code matches the secret for the current time step (±1 step drift). */
    public boolean verify(String secret, String code) {
        if (secret == null || code == null || !code.matches("^[0-9]{6}$")) return false;
        long currentStep = Instant.now().getEpochSecond() / TIME_STEP_SECONDS;
        for (int drift = -ALLOWED_DRIFT_STEPS; drift <= ALLOWED_DRIFT_STEPS; drift++) {
            if (code.equals(generateCode(secret, currentStep + drift))) return true;
        }
        return false;
    }

    private String generateCode(String base32Secret, long timeStep) {
        try {
            byte[] key = base32Decode(base32Secret);
            byte[] data = new byte[8];
            long value = timeStep;
            for (int i = 7; i >= 0; i--) { data[i] = (byte) (value & 0xff); value >>= 8; }

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0f;
            int binary = ((hash[offset] & 0x7f) << 24)
                    | ((hash[offset + 1] & 0xff) << 16)
                    | ((hash[offset + 2] & 0xff) << 8)
                    | (hash[offset + 3] & 0xff);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception e) {
            throw new IllegalStateException("Could not compute TOTP code", e);
        }
    }

    private String base32Encode(byte[] data) {
        StringBuilder sb = new StringBuilder();
        int bits = 0, value = 0;
        for (byte b : data) {
            value = (value << 8) | (b & 0xff);
            bits += 8;
            while (bits >= 5) {
                sb.append(BASE32_ALPHABET[(value >>> (bits - 5)) & 0x1f]);
                bits -= 5;
            }
        }
        if (bits > 0) {
            sb.append(BASE32_ALPHABET[(value << (5 - bits)) & 0x1f]);
        }
        return sb.toString();
    }

    private byte[] base32Decode(String encoded) {
        String clean = encoded.trim().toUpperCase().replace("=", "");
        long bits = 0;
        int bitCount = 0;
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        for (char c : clean.toCharArray()) {
            int idx = new String(BASE32_ALPHABET).indexOf(c);
            if (idx < 0) continue;
            bits = (bits << 5) | idx;
            bitCount += 5;
            if (bitCount >= 8) {
                out.write((int) ((bits >> (bitCount - 8)) & 0xff));
                bitCount -= 8;
            }
        }
        return out.toByteArray();
    }
}
