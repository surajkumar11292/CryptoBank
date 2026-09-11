package com.cryptobank.web.dto.response;

import java.util.List;

public record AiChatResponse(
        String reply,
        String suggestedAction,
        List<String> quickFollowUps,
        String engine,
        String timestamp
) {}
