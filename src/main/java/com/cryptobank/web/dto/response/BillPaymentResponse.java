package com.cryptobank.web.dto.response;

import com.cryptobank.domain.entity.BillPaymentEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record BillPaymentResponse(
        Long id,
        Long accountNumber,
        String category,
        String consumer,
        BigDecimal amount,
        String reference,
        Instant createdAt
) {
    public static BillPaymentResponse from(BillPaymentEntity bp) {
        return new BillPaymentResponse(
                bp.getId(),
                bp.getAccount().getAccountNumber(),
                bp.getCategory(),
                bp.getConsumer(),
                bp.getAmount(),
                bp.getReference(),
                bp.getCreatedAt()
        );
    }
}
