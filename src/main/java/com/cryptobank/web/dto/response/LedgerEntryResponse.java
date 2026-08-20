package com.cryptobank.web.dto.response;

import com.cryptobank.domain.entity.LedgerEntryEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryResponse(
        Long id,
        String type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        String description,
        String reference,
        Instant createdAt
) {
    public static LedgerEntryResponse from(LedgerEntryEntity e) {
        return new LedgerEntryResponse(
                e.getId(),
                e.getType().name(),
                e.getAmount(),
                e.getBalanceAfter(),
                e.getDescription(),
                e.getReference(),
                e.getCreatedAt()
        );
    }
}
