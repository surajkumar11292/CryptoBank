package com.cryptobank.service.impl;

import com.cryptobank.domain.entity.AccountEntity;
import com.cryptobank.domain.entity.CardEntity;
import com.cryptobank.domain.entity.UserEntity;
import com.cryptobank.repository.AccountRepository;
import com.cryptobank.repository.CardRepository;
import com.cryptobank.service.CardService;
import com.cryptobank.web.dto.request.CardUpdateRequest;
import com.cryptobank.web.dto.response.CardResponse;
import com.cryptobank.web.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class CardServiceImpl implements CardService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final CardRepository cards;
    private final AccountRepository accounts;

    public CardServiceImpl(CardRepository cards, AccountRepository accounts) {
        this.cards = cards;
        this.accounts = accounts;
    }

    @Override
    @Transactional
    public List<CardResponse> mine(UserEntity owner) {
        List<AccountEntity> owned = accounts.findByOwnerIdOrderByCreatedAtDesc(owner.getId());
        return owned.stream().map(this::getOrCreate).map(CardResponse::from).toList();
    }

    @Override
    @Transactional
    public CardResponse update(UserEntity owner, Long accountNumber, CardUpdateRequest req) {
        CardEntity card = getOrCreateOwned(owner, accountNumber);
        if (req.frozen() != null) card.setFrozen(req.frozen());
        if (req.contactlessEnabled() != null) card.setContactlessEnabled(req.contactlessEnabled());
        if (req.onlineEnabled() != null) card.setOnlineEnabled(req.onlineEnabled());
        return CardResponse.from(card);
    }

    @Override
    @Transactional
    public CardResponse requestReplacement(UserEntity owner, Long accountNumber) {
        CardEntity card = getOrCreateOwned(owner, accountNumber);
        card.setReplacementRequestedAt(Instant.now());
        return CardResponse.from(card);
    }

    private CardEntity getOrCreateOwned(UserEntity owner, Long accountNumber) {
        AccountEntity a = accounts.findByAccountNumber(accountNumber)
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        if (!a.getOwner().getId().equals(owner.getId())) {
            throw ApiException.forbidden("You don't own this account");
        }
        return getOrCreate(a);
    }

    private CardEntity getOrCreate(AccountEntity a) {
        return cards.findByAccountId(a.getId()).orElseGet(() -> {
            CardEntity c = new CardEntity();
            c.setAccount(a);
            c.setCardNumber(generateCardNumber());
            var expiry = Instant.now().atZone(ZoneOffset.UTC).plusYears(4);
            c.setExpiryMonth(expiry.getMonthValue());
            c.setExpiryYear(expiry.getYear());
            return cards.save(c);
        });
    }

    private String generateCardNumber() {
        // Standard 16-digit card number generation: 4532 (Visa BIN) + 12 digits
        StringBuilder sb = new StringBuilder("4532");
        for (int i = 0; i < 12; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
