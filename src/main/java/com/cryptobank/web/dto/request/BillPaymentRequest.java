package com.cryptobank.web.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record BillPaymentRequest(
        @NotNull(message = "Account number is required")
        Long accountNumber,

        @NotBlank(message = "Bill category is required")
        String category,

        @NotBlank(message = "Consumer ID/number is required")
        String consumer,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1.00", message = "Amount must be at least ₹1.00")
        BigDecimal amount,

        @NotBlank(message = "Security PIN is required")
        @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be 4 digits")
        String pin
) {}
