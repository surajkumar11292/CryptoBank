package com.cryptobank.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BeneficiaryRequest(
        @NotBlank(message = "Beneficiary name is required")
        @Size(min = 2, max = 100)
        String name,

        @NotNull(message = "Account number is required")
        Long accountNumber,

        @Size(max = 60)
        String nickname
) {}
