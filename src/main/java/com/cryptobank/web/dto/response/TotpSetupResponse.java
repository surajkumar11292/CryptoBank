package com.cryptobank.web.dto.response;

public record TotpSetupResponse(
        String secret,
        String otpAuthUri
) {}
