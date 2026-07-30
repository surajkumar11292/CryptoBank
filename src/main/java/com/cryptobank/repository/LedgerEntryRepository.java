package com.cryptobank.repository;

import com.cryptobank.domain.entity.LedgerEntryEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntryEntity, Long> {

    Page<LedgerEntryEntity> findByAccountIdOrderByCreatedAtDesc(Long accountId, Pageable pageable);

    List<LedgerEntryEntity> findByAccountIdOrderByCreatedAtDesc(Long accountId);
}
