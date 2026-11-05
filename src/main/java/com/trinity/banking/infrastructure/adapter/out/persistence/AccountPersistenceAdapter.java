package com.trinity.banking.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.trinity.banking.domain.model.Account;
import com.trinity.banking.domain.port.out.AccountRepositoryPort;
import com.trinity.banking.infrastructure.adapter.out.persistence.repository.AccountJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository jpa;

    @Override
    public Account save(Account account) {
        return PersistenceMapper.toDomain(jpa.save(PersistenceMapper.toEntity(account)));
    }

    @Override
    public Optional<Account> findById(Long id) {
        return jpa.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public Optional<Account> findByIdForUpdate(Long id) {
        return jpa.findByIdForUpdate(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumberForUpdate(String accountNumber) {
        return jpa.findByAccountNumberForUpdate(accountNumber).map(PersistenceMapper::toDomain);
    }

    @Override
    public List<Account> findAll() {
        return jpa.findAll().stream().map(PersistenceMapper::toDomain).toList();
    }

    @Override
    public List<Account> findByClientId(Long clientId) {
        return jpa.findByClientId(clientId).stream().map(PersistenceMapper::toDomain).toList();
    }

    @Override
    public boolean existsByClientId(Long clientId) {
        return jpa.existsByClientId(clientId);
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return jpa.existsByAccountNumber(accountNumber);
    }
}
