package com.cryptobank.web.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AiInsightResponse(
        int healthScore,
        String healthGrade,
        String summary,
        List<String> bulletInsights,
        String topExpenseCategory,
        BigDecimal monthlyBurnRate,
        BigDecimal monthlyInflow,
        BigDecimal netSavingsRatio,
        String engine,
        String generatedAt
) {}
