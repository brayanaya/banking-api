package com.trinity.banking.infrastructure.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.trinity.banking.infrastructure.adapter.out.persistence.entity.AccountEntity;

import jakarta.persistence.LockModeType;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.id = :id")
    Optional<AccountEntity> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from AccountEntity a where a.accountNumber = :accountNumber")
    Optional<AccountEntity> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    List<AccountEntity> findByClientId(Long clientId);

    boolean existsByClientId(Long clientId);

    boolean existsByAccountNumber(String accountNumber);
}
