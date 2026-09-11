package com.cryptobank.service;

import com.cryptobank.domain.entity.SupportTicketEntity;
import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.web.dto.request.AiChatRequest;
import com.cryptobank.web.dto.request.TransferRiskRequest;
import com.cryptobank.web.dto.response.AiChatResponse;
import com.cryptobank.web.dto.response.AiInsightResponse;
import com.cryptobank.web.dto.response.TransferRiskResponse;

public interface AiService {

    /**
     * Interactive conversational financial advisor powered by Gemini 3.6 Flash.
     * Grounded on user's real account balances, recent ledger history, and bill payments.
     */
    AiChatResponse chat(UserEntity user, AiChatRequest request);

    /**
     * Synthesizes last 30 days of transactions into financial health metrics,
     * monthly burn rate, top expense category, and personalized recommendations.
     */
    AiInsightResponse getMonthlyInsights(UserEntity user, Long accountNumber);

    /**
     * Real-time transfer fraud and anomaly assessment before money movement.
     * Evaluates transaction velocity, recipient history, and amount deviation.
     */
    TransferRiskResponse assessTransferRisk(UserEntity user, TransferRiskRequest request);

    /**
     * Generates a customer support triage categorization and professional resolution draft.
     */
    String generateTicketResolutionDraft(SupportTicketEntity ticket);
}
