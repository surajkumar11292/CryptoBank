package com.cryptobank.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SupportTicketRequest(
        @NotBlank(message = "Subject is required")
        @Size(max = 150)
        String subject,

        @NotBlank(message = "Message is required")
        String message
) {}
