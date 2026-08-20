package com.cryptobank.web.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record OpenAccountRequest(
        @NotBlank(message = "Holder name is required")
        String holderName,

        @NotNull(message = "Opening balance is required")
        @DecimalMin(value = "0.00", message = "Opening balance cannot be negative")
        BigDecimal openingBalance,

        @NotBlank(message = "PIN is required")
        @Pattern(regexp = "^[0-9]{4}$", message = "PIN must be exactly 4 digits")
        String pin
) {}
