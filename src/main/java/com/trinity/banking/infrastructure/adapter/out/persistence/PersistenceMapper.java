package com.trinity.banking.infrastructure.adapter.out.persistence;

import com.trinity.banking.domain.model.Account;
import com.trinity.banking.domain.model.Client;
import com.trinity.banking.domain.model.Transaction;
import com.trinity.banking.infrastructure.adapter.out.persistence.entity.AccountEntity;
import com.trinity.banking.infrastructure.adapter.out.persistence.entity.ClientEntity;
import com.trinity.banking.infrastructure.adapter.out.persistence.entity.TransactionEntity;

/** Traduce entre el modelo de dominio y las entidades JPA (el dominio no conoce JPA). */
public final class PersistenceMapper {

    private PersistenceMapper() {
    }

    public static Client toDomain(ClientEntity e) {
        return Client.builder()
                .id(e.getId())
                .identificationType(e.getIdentificationType())
                .identificationNumber(e.getIdentificationNumber())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .birthDate(e.getBirthDate())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    public static ClientEntity toEntity(Client c) {
        return ClientEntity.builder()
                .id(c.getId())
                .identificationType(c.getIdentificationType())
                .identificationNumber(c.getIdentificationNumber())
                .firstName(c.getFirstName())
                .lastName(c.getLastName())
                .email(c.getEmail())
                .birthDate(c.getBirthDate())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    public static Account toDomain(AccountEntity e) {
        return Account.builder()
                .id(e.getId())
                .type(e.getAccountType())
                .accountNumber(e.getAccountNumber())
                .status(e.getStatus())
                .balance(e.getBalance())
                .availableBalance(e.getAvailableBalance())
                .gmfExempt(e.isGmfExempt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .clientId(e.getClientId())
                .build();
    }

    public static AccountEntity toEntity(Account a) {
        return AccountEntity.builder()
                .id(a.getId())
                .accountType(a.getType())
                .accountNumber(a.getAccountNumber())
                .status(a.getStatus())
                .balance(a.getBalance())
                .availableBalance(a.getAvailableBalance())
                .gmfExempt(a.isGmfExempt())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .clientId(a.getClientId())
                .build();
    }

    public static Transaction toDomain(TransactionEntity e) {
        return Transaction.builder()
                .id(e.getId())
                .reference(e.getReference())
                .transactionType(e.getTransactionType())
                .movementType(e.getMovementType())
                .amount(e.getAmount())
                .balanceAfter(e.getBalanceAfter())
                .availableBalanceAfter(e.getAvailableBalanceAfter())
                .accountId(e.getAccountId())
                .accountNumber(e.getAccountNumber())
                .relatedAccountNumber(e.getRelatedAccountNumber())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .build();
    }

    public static TransactionEntity toEntity(Transaction t) {
        return TransactionEntity.builder()
                .id(t.getId())
                .reference(t.getReference())
                .transactionType(t.getTransactionType())
                .movementType(t.getMovementType())
                .amount(t.getAmount())
                .balanceAfter(t.getBalanceAfter())
                .availableBalanceAfter(t.getAvailableBalanceAfter())
                .accountId(t.getAccountId())
                .accountNumber(t.getAccountNumber())
                .relatedAccountNumber(t.getRelatedAccountNumber())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
