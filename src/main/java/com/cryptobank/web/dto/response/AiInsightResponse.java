package com.cryptobank.web.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AiInsightResponse(
        int healthScore,
        String healthGrade,
        String summary,
        List<String> bulletInsights,
        String topExpenseCategory,
        BigDecimal monthlyBurnRate,
        BigDecimal monthlyInflow,
        BigDecimal netSavingsRatio,
        Map<String, BigDecimal> categoryBreakdown,
        String engine,
        String generatedAt
) {}
