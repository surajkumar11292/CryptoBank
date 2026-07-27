package com.cryptobank.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "cards", indexes = {
    @Index(name = "idx_card_account", columnList = "account_id")
})
@Getter
@Setter
@NoArgsConstructor
public class CardEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(nullable = false, length = 19)
    private String cardNumber;

    @Column(nullable = false)
    private int expiryMonth;

    @Column(nullable = false)
    private int expiryYear;

    @Column(nullable = false)
    private boolean frozen = false;

    @Column(nullable = false)
    private boolean contactlessEnabled = true;

    @Column(nullable = false)
    private boolean onlineEnabled = true;

    private Instant replacementRequestedAt;
}
