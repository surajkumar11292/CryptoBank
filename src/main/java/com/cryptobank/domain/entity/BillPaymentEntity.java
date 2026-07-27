package com.cryptobank.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "bill_payments", indexes = {
    @Index(name = "idx_bill_owner", columnList = "owner_id"),
    @Index(name = "idx_bill_account", columnList = "account_id")
})
@Getter
@Setter
@NoArgsConstructor
public class BillPaymentEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private AccountEntity account;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 120)
    private String consumer;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 64)
    private String reference;
}
