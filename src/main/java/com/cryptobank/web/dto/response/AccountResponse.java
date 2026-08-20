package com.cryptobank.web.dto.response;

import com.cryptobank.domain.entity.AccountEntity;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        Long accountNumber,
        String holderName,
        BigDecimal balance,
        boolean frozen,
        Instant createdAt
) {
    public static AccountResponse from(AccountEntity a) {
        return new AccountResponse(
                a.getId(),
                a.getAccountNumber(),
                a.getHolderName(),
                a.getBalance(),
                a.isFrozen(),
                a.getCreatedAt()
        );
    }
}
