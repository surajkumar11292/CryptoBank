package com.cryptobank.web.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TotpCodeRequest(
        @NotBlank(message = "TOTP code is required")
        @Pattern(regexp = "^[0-9]{6}$", message = "Code must be 6 digits")
        String code
) {}
