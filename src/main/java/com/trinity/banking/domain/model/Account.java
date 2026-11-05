package com.trinity.banking.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.trinity.banking.domain.exception.BusinessRuleException;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Producto financiero (cuenta corriente o de ahorros). Concentra las reglas de negocio
 * sobre saldos y estados para que ningún servicio pueda dejarla en un estado inválido.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    /** Cupo de sobregiro de la cuenta corriente. La cuenta de ahorros nunca puede quedar bajo $0. */
    public static final BigDecimal CHECKING_OVERDRAFT_LIMIT = new BigDecimal("1000000.00");

    private Long id;
    private AccountType type;
    private String accountNumber;
    private AccountStatus status;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private boolean gmfExempt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long clientId;

    /** Saldo disponible: ahorros = saldo; corriente = saldo + cupo de sobregiro. */
    public void refreshAvailableBalance() {
        this.availableBalance = type == AccountType.CHECKING
                ? balance.add(CHECKING_OVERDRAFT_LIMIT)
                : balance;
    }

    public void credit(BigDecimal amount) {
        ensureActive();
        requirePositive(amount);
        this.balance = balance.add(amount);
        refreshAvailableBalance();
    }

    public void debit(BigDecimal amount) {
        ensureActive();
        requirePositive(amount);
        if (amount.compareTo(availableBalance) > 0) {
            throw new BusinessRuleException("Fondos insuficientes en la cuenta " + accountNumber);
        }
        this.balance = balance.subtract(amount);
        refreshAvailableBalance();
    }

    public void changeStatus(AccountStatus target) {
        if (status == AccountStatus.CANCELLED) {
            throw new BusinessRuleException("Una cuenta cancelada no puede cambiar de estado");
        }
        if (target == AccountStatus.CANCELLED && balance.compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessRuleException("Solo se pueden cancelar cuentas con saldo igual a $0");
        }
        this.status = target;
    }

    private void ensureActive() {
        if (status != AccountStatus.ACTIVE) {
            throw new BusinessRuleException(
                    "La cuenta " + accountNumber + " no está activa (estado: " + status + ")");
        }
    }

    private static void requirePositive(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("El monto debe ser mayor a cero");
        }
    }
}
