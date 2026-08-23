package com.cryptobank.web.dto.request;

public record CardUpdateRequest(
        Boolean frozen,
        Boolean contactlessEnabled,
        Boolean onlineEnabled
) {}
