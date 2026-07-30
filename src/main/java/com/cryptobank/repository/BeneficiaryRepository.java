package com.cryptobank.repository;

import com.cryptobank.domain.entity.BeneficiaryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BeneficiaryRepository extends JpaRepository<BeneficiaryEntity, Long> {
    List<BeneficiaryEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
}
