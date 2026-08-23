package com.cryptobank.web.dto.response;

import com.cryptobank.domain.entity.BeneficiaryEntity;

import java.time.Instant;

public record BeneficiaryResponse(
        Long id,
        String name,
        Long accountNumber,
        String nickname,
        Instant createdAt
) {
    public static BeneficiaryResponse from(BeneficiaryEntity b) {
        return new BeneficiaryResponse(
                b.getId(),
                b.getName(),
                b.getAccountNumber(),
                b.getNickname(),
                b.getCreatedAt()
        );
    }
}
