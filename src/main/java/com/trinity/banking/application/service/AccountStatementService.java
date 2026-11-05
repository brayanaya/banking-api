package com.trinity.banking.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trinity.banking.domain.exception.BusinessRuleException;
import com.trinity.banking.domain.exception.ResourceNotFoundException;
import com.trinity.banking.domain.model.Account;
import com.trinity.banking.domain.model.AccountStatement;
import com.trinity.banking.domain.model.MovementType;
import com.trinity.banking.domain.model.Transaction;
import com.trinity.banking.domain.port.in.AccountStatementUseCase;
import com.trinity.banking.domain.port.out.AccountRepositoryPort;
import com.trinity.banking.domain.port.out.TransactionRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccountStatementService implements AccountStatementUseCase {

    private final AccountRepositoryPort accountRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final Clock clock;

    @Override
    public AccountStatement getStatement(Long accountId, LocalDate from, LocalDate to) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta con id " + accountId + " no encontrada"));

        LocalDate end = to != null ? to : LocalDate.now(clock);
        LocalDate start = from != null ? from : end.withDayOfMonth(1);
        if (start.isAfter(end)) {
            throw new BusinessRuleException("La fecha inicial no puede ser posterior a la fecha final");
        }

        List<Transaction> movements = transactionRepository.findByAccountIdBetween(
                accountId, start.atStartOfDay(), end.plusDays(1).atStartOfDay());

        return new AccountStatement(account, start, end, movements,
                total(movements, MovementType.CREDIT), total(movements, MovementType.DEBIT));
    }

    private static BigDecimal total(List<Transaction> movements, MovementType type) {
        return movements.stream()
                .filter(m -> m.getMovementType() == type)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
