package com.trinity.banking.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trinity.banking.domain.exception.BusinessRuleException;
import com.trinity.banking.domain.exception.ResourceNotFoundException;
import com.trinity.banking.domain.model.Account;
import com.trinity.banking.domain.model.MovementType;
import com.trinity.banking.domain.model.Transaction;
import com.trinity.banking.domain.model.TransactionType;
import com.trinity.banking.domain.port.in.TransactionUseCase;
import com.trinity.banking.domain.port.out.AccountRepositoryPort;
import com.trinity.banking.domain.port.out.TransactionRepositoryPort;

import lombok.RequiredArgsConstructor;

/**
 * Cada operación es una única transacción ACID: o se aplican todos los cambios
 * (saldos + movimientos) o no se aplica ninguno. Las cuentas se bloquean con
 * SELECT ... FOR UPDATE para evitar condiciones de carrera.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService implements TransactionUseCase {

    private final AccountRepositoryPort accountRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final Clock clock;

    @Override
    public Transaction deposit(String accountNumber, BigDecimal amount, String description) {
        requirePositive(amount);
        Account account = lock(accountNumber);
        LocalDateTime now = LocalDateTime.now(clock);

        account.credit(amount);
        account.setUpdatedAt(now);
        accountRepository.save(account);

        return transactionRepository.save(movement(account, TransactionType.DEPOSIT, MovementType.CREDIT,
                amount, null, UUID.randomUUID().toString(), description, now));
    }

    @Override
    public Transaction withdraw(String accountNumber, BigDecimal amount, String description) {
        requirePositive(amount);
        Account account = lock(accountNumber);
        LocalDateTime now = LocalDateTime.now(clock);

        account.debit(amount);
        account.setUpdatedAt(now);
        accountRepository.save(account);

        return transactionRepository.save(movement(account, TransactionType.WITHDRAWAL, MovementType.DEBIT,
                amount, null, UUID.randomUUID().toString(), description, now));
    }

    @Override
    public List<Transaction> transfer(String originNumber, String destinationNumber,
                                      BigDecimal amount, String description) {
        requirePositive(amount);
        if (originNumber.equals(destinationNumber)) {
            throw new BusinessRuleException("La cuenta origen y la cuenta destino no pueden ser la misma");
        }

        // Se bloquea siempre en el mismo orden (por número) para evitar deadlocks entre transferencias cruzadas
        String first = originNumber.compareTo(destinationNumber) < 0 ? originNumber : destinationNumber;
        String second = first.equals(originNumber) ? destinationNumber : originNumber;
        Account firstAccount = lock(first);
        Account secondAccount = lock(second);
        Account origin = first.equals(originNumber) ? firstAccount : secondAccount;
        Account destination = first.equals(originNumber) ? secondAccount : firstAccount;

        LocalDateTime now = LocalDateTime.now(clock);
        origin.debit(amount);
        destination.credit(amount);
        origin.setUpdatedAt(now);
        destination.setUpdatedAt(now);
        accountRepository.save(origin);
        accountRepository.save(destination);

        String reference = UUID.randomUUID().toString();
        Transaction debit = transactionRepository.save(movement(origin, TransactionType.TRANSFER,
                MovementType.DEBIT, amount, destination.getAccountNumber(), reference, description, now));
        Transaction credit = transactionRepository.save(movement(destination, TransactionType.TRANSFER,
                MovementType.CREDIT, amount, origin.getAccountNumber(), reference, description, now));
        return List.of(debit, credit);
    }

    // ------------------------------------------------------------------ helpers

    private Account lock(String accountNumber) {
        return accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cuenta " + accountNumber + " no encontrada"));
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("El monto debe ser mayor a cero");
        }
    }

    private static Transaction movement(Account account, TransactionType type, MovementType movementType,
                                        BigDecimal amount, String relatedAccountNumber, String reference,
                                        String description, LocalDateTime now) {
        return Transaction.builder()
                .reference(reference)
                .transactionType(type)
                .movementType(movementType)
                .amount(amount)
                .balanceAfter(account.getBalance())
                .availableBalanceAfter(account.getAvailableBalance())
                .accountId(account.getId())
                .accountNumber(account.getAccountNumber())
                .relatedAccountNumber(relatedAccountNumber)
                .description(description)
                .createdAt(now)
                .build();
    }
}
