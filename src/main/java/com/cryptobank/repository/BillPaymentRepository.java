package com.cryptobank.repository;

import com.cryptobank.domain.entity.BillPaymentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BillPaymentRepository extends JpaRepository<BillPaymentEntity, Long> {
    Page<BillPaymentEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId, Pageable pageable);
}
