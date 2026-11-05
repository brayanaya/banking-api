package com.trinity.banking.application.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trinity.banking.domain.exception.BusinessRuleException;
import com.trinity.banking.domain.exception.ResourceNotFoundException;
import com.trinity.banking.domain.model.Account;
import com.trinity.banking.domain.model.AccountStatus;
import com.trinity.banking.domain.model.AccountType;
import com.trinity.banking.domain.model.MovementType;
import com.trinity.banking.domain.model.Transaction;
import com.trinity.banking.domain.model.TransactionType;
import com.trinity.banking.domain.port.in.AccountUseCase;
import com.trinity.banking.domain.port.in.CreateAccountCommand;
import com.trinity.banking.domain.port.out.AccountRepositoryPort;
import com.trinity.banking.domain.port.out.ClientRepositoryPort;
import com.trinity.banking.domain.port.out.TransactionRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService implements AccountUseCase {

    private static final int NUMBER_LENGTH = 10;

    private final AccountRepositoryPort accountRepository;
    private final ClientRepositoryPort clientRepository;
    private final TransactionRepositoryPort transactionRepository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Override
    public Account create(CreateAccountCommand command) {
        // Un producto solo puede existir si está vinculado a un cliente de la entidad
        clientRepository.findById(command.clientId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cliente con id " + command.clientId() + " no encontrado"));

        BigDecimal initialBalance = command.initialBalance() == null
                ? BigDecimal.ZERO.setScale(2)
                : command.initialBalance();
        if (initialBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessRuleException("El saldo inicial no puede ser menor a $0");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        Account account = Account.builder()
                .type(command.type())
                .accountNumber(generateAccountNumber(command.type()))
                .status(AccountStatus.ACTIVE)          // toda cuenta nace activa
                .balance(initialBalance)
                .gmfExempt(command.gmfExempt())
                .createdAt(now)                        // fecha de creación automática
                .clientId(command.clientId())
                .build();
        account.refreshAvailableBalance();
        Account saved = accountRepository.save(account);

        if (initialBalance.compareTo(BigDecimal.ZERO) > 0) {
            transactionRepository.save(Transaction.builder()
                    .reference(UUID.randomUUID().toString())
                    .transactionType(TransactionType.DEPOSIT)
                    .movementType(MovementType.CREDIT)
                    .amount(initialBalance)
                    .balanceAfter(saved.getBalance())
                    .availableBalanceAfter(saved.getAvailableBalance())
                    .accountId(saved.getId())
                    .accountNumber(saved.getAccountNumber())
                    .description("Saldo inicial")
                    .createdAt(now)
                    .build());
        }
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Account getById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta con id " + id + " no encontrada"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Account> getAll(Long clientId) {
        return clientId == null ? accountRepository.findAll() : accountRepository.findByClientId(clientId);
    }

    @Override
    public Account changeStatus(Long id, AccountStatus status) {
        Account account = accountRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta con id " + id + " no encontrada"));
        account.changeStatus(status);
        account.setUpdatedAt(LocalDateTime.now(clock));
        return accountRepository.save(account);
    }

    /** Genera un número único de 10 dígitos: prefijo (53 ahorros / 33 corriente) + 8 dígitos aleatorios. */
    private String generateAccountNumber(AccountType type) {
        String prefix = type.getNumberPrefix();
        int randomDigits = NUMBER_LENGTH - prefix.length();
        int bound = (int) Math.pow(10, randomDigits);
        String number;
        do {
            number = prefix + String.format("%0" + randomDigits + "d", random.nextInt(bound));
        } while (accountRepository.existsByAccountNumber(number));
        return number;
    }
}
