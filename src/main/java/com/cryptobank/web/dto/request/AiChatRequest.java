package com.cryptobank.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        @NotBlank(message = "Message cannot be empty")
        @Size(max = 1000, message = "Message exceeds 1,000 characters limit")
        String message,

        Long accountNumber
) {}
