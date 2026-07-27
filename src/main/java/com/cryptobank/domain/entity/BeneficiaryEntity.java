package com.cryptobank.domain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "beneficiaries", indexes = {
    @Index(name = "idx_beneficiary_owner", columnList = "owner_id")
})
@Getter
@Setter
@NoArgsConstructor
public class BeneficiaryEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private Long accountNumber;

    @Column(length = 60)
    private String nickname;
}
