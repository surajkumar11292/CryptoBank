package com.cryptobank.web.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record MoneyRequest(
        @NotNull(message = "Account number is required")
        Long accountNumber,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "1.00", message = "Amount must be at least ₹1.00")
        BigDecimal amount,

        @NotBlank(message = "Security PIN is required")
        @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be 4 digits")
        String pin
) {}
