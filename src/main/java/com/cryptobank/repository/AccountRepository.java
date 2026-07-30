package com.cryptobank.repository;

import com.cryptobank.domain.entity.AccountEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByAccountNumber(Long accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM AccountEntity a WHERE a.accountNumber = :number")
    Optional<AccountEntity> lockByAccountNumber(@Param("number") Long number);

    List<AccountEntity> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    boolean existsByAccountNumber(Long accountNumber);
}
