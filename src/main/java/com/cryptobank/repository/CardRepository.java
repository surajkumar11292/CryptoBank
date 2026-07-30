package com.cryptobank.repository;

import com.cryptobank.domain.entity.CardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardRepository extends JpaRepository<CardEntity, Long> {
    Optional<CardEntity> findByAccountId(Long accountId);
}
