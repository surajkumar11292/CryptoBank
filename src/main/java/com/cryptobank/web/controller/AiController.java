package com.cryptobank.web.controller;

import com.cryptobank.domain.entity.SupportTicketEntity;
import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.repository.SupportTicketRepository;
import com.cryptobank.security.SecurityUtils;
import com.cryptobank.service.AiService;
import com.cryptobank.web.dto.request.AiChatRequest;
import com.cryptobank.web.dto.request.TransferRiskRequest;
import com.cryptobank.web.dto.response.AiChatResponse;
import com.cryptobank.web.dto.response.AiInsightResponse;
import com.cryptobank.web.dto.response.TransferRiskResponse;
import com.cryptobank.web.exception.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI Financial Intelligence", description = "Endpoints for Gemini-powered financial advisor, spending insights, and risk scoring")
public class AiController {

    private final AiService aiService;
    private final SecurityUtils securityUtils;
    private final SupportTicketRepository supportTicketRepository;

    @PostMapping("/chat")
    @Operation(summary = "Ask AI Financial Advisor", description = "Conversational financial advisor grounded on customer's real banking ledger and balances")
    public ResponseEntity<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        UserEntity user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(aiService.chat(user, request));
    }

    @GetMapping("/insights")
    @Operation(summary = "Get Monthly Spending & Financial Health Insights", description = "Generates financial health score, 30-day burn rate, and AI optimization recommendations")
    public ResponseEntity<AiInsightResponse> getInsights(@RequestParam(required = false) Long accountNumber) {
        UserEntity user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(aiService.getMonthlyInsights(user, accountNumber));
    }

    @PostMapping("/assess-transfer-risk")
    @Operation(summary = "Real-Time Transfer Anomaly & Fraud Assessment", description = "Evaluates transfer risk level and anomaly score prior to transaction confirmation")
    public ResponseEntity<TransferRiskResponse> assessTransferRisk(@Valid @RequestBody TransferRiskRequest request) {
        UserEntity user = securityUtils.getCurrentUser();
        return ResponseEntity.ok(aiService.assessTransferRisk(user, request));
    }

    @PostMapping("/ticket-draft/{ticketId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate AI Support Resolution Draft", description = "Admin-only: Generates an AI-drafted response for triage and ticket closing")
    public ResponseEntity<Map<String, String>> generateTicketDraft(@PathVariable Long ticketId) {
        SupportTicketEntity ticket = supportTicketRepository.findById(ticketId)
                .orElseThrow(() -> ApiException.notFound("Ticket #" + ticketId + " not found"));

        String draft = aiService.generateTicketResolutionDraft(ticket);
        ticket.setAiDraftReply(draft);
        supportTicketRepository.save(ticket);

        return ResponseEntity.ok(Map.of("draftReply", draft));
    }
}
