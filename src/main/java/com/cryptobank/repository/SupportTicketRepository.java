package com.cryptobank.repository;

import com.cryptobank.domain.entity.SupportTicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicketEntity, Long> {
    List<SupportTicketEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);
    List<SupportTicketEntity> findAllByOrderByCreatedAtDesc();
}
