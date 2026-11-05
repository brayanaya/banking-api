package com.trinity.banking.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.trinity.banking.domain.model.Account;

public interface AccountRepositoryPort {

    Account save(Account account);

    Optional<Account> findById(Long id);

    /** Bloqueo pesimista de escritura: garantiza aislamiento al mover saldos. */
    Optional<Account> findByIdForUpdate(Long id);

    Optional<Account> findByAccountNumberForUpdate(String accountNumber);

    List<Account> findAll();

    List<Account> findByClientId(Long clientId);

    boolean existsByClientId(Long clientId);

    boolean existsByAccountNumber(String accountNumber);
}
