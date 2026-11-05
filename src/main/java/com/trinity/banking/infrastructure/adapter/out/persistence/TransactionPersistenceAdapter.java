package com.trinity.banking.infrastructure.adapter.out.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.trinity.banking.domain.model.Transaction;
import com.trinity.banking.domain.port.out.TransactionRepositoryPort;
import com.trinity.banking.infrastructure.adapter.out.persistence.repository.TransactionJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TransactionPersistenceAdapter implements TransactionRepositoryPort {

    private final TransactionJpaRepository jpa;

    @Override
    public Transaction save(Transaction transaction) {
        return PersistenceMapper.toDomain(jpa.save(PersistenceMapper.toEntity(transaction)));
    }

    @Override
    public List<Transaction> findByAccountIdBetween(Long accountId, LocalDateTime from, LocalDateTime toExclusive) {
        return jpa.findStatement(accountId, from, toExclusive).stream().map(PersistenceMapper::toDomain).toList();
    }
}
