package com.cryptobank.web.dto.response;

import com.cryptobank.domain.entity.SupportTicketEntity;

import java.time.Instant;

public record AdminTicketResponse(
        Long id,
        Long userId,
        String userFullName,
        String userEmail,
        String subject,
        String message,
        String status,
        String aiCategory,
        String aiDraftReply,
        Instant createdAt
) {
    public static AdminTicketResponse from(SupportTicketEntity t) {
        return new AdminTicketResponse(
                t.getId(),
                t.getOwner().getId(),
                t.getOwner().getFullName(),
                t.getOwner().getEmail(),
                t.getSubject(),
                t.getMessage(),
                t.getStatus().name(),
                t.getAiCategory(),
                t.getAiDraftReply(),
                t.getCreatedAt()
        );
    }
}
