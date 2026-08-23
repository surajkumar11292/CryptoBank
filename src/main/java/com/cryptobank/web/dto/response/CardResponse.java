package com.cryptobank.web.dto.response;

import com.cryptobank.domain.entity.CardEntity;

import java.time.Instant;

public record CardResponse(
        Long id,
        Long accountNumber,
        String holderName,
        String maskedNumber,
        int expiryMonth,
        int expiryYear,
        boolean frozen,
        boolean contactlessEnabled,
        boolean onlineEnabled,
        Instant replacementRequestedAt
) {
    public static CardResponse from(CardEntity c) {
        String num = c.getCardNumber();
        String last4 = num.length() >= 4 ? num.substring(num.length() - 4) : "0000";
        String masked = "•••• •••• •••• " + last4;
        return new CardResponse(
                c.getId(),
                c.getAccount().getAccountNumber(),
                c.getAccount().getHolderName(),
                masked,
                c.getExpiryMonth(),
                c.getExpiryYear(),
                c.isFrozen(),
                c.isContactlessEnabled(),
                c.isOnlineEnabled(),
                c.getReplacementRequestedAt()
        );
    }
}
