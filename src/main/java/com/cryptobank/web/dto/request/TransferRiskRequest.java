package com.cryptobank.web.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record TransferRiskRequest(
        @NotNull(message = "Source account is required")
        Long fromAccountNumber,

        @NotNull(message = "Destination account is required")
        Long toAccountNumber,

        @NotNull(message = "Transfer amount is required")
        @Positive(message = "Transfer amount must be positive")
        BigDecimal amount
) {}
