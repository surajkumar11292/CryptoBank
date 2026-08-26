package com.cryptobank.web.dto.response;

import com.cryptobank.domain.entity.SupportTicketEntity;

import java.time.Instant;

public record SupportTicketResponse(
        Long id,
        String subject,
        String message,
        String status,
        Instant createdAt
) {
    public static SupportTicketResponse from(SupportTicketEntity t) {
        return new SupportTicketResponse(
                t.getId(),
                t.getSubject(),
                t.getMessage(),
                t.getStatus().name(),
                t.getCreatedAt()
        );
    }
}
