package com.cryptobank.web.dto.response;

public record TransferRiskResponse(
        String riskLevel, // LOW, MEDIUM, HIGH
        int riskScore,    // 0 to 100
        String analysis,
        boolean isNewBeneficiary,
        boolean isUnusualAmount,
        String recommendation,
        String engine
) {}
