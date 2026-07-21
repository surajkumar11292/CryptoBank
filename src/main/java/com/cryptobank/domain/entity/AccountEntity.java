package com.cryptobank.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "accounts", indexes = {
    @Index(name = "idx_accounts_number", columnList = "accountNumber", unique = true),
    @Index(name = "idx_accounts_owner", columnList = "owner_id")
})
@Getter
@Setter
@NoArgsConstructor
public class AccountEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(nullable = false, unique = true)
    private Long accountNumber;

    @Column(nullable = false, length = 100)
    private String holderName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(nullable = false, length = 100)
    private String pinHash;

    @Column(nullable = false)
    private boolean frozen = false;
}
