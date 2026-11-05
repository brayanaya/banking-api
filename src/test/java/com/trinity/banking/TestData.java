package com.trinity.banking;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.trinity.banking.domain.model.Account;
import com.trinity.banking.domain.model.AccountStatus;
import com.trinity.banking.domain.model.AccountType;
import com.trinity.banking.domain.model.Client;
import com.trinity.banking.domain.model.IdentificationType;

/** Fábrica de objetos de prueba compartida por los tests. */
public final class TestData {

    public static final ZoneId ZONE = ZoneId.of("America/Bogota");
    /** 2026-11-03 10:00 hora Colombia */
    public static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-03T15:00:00Z"), ZONE);
    public static final LocalDate TODAY = LocalDate.now(CLOCK);
    public static final LocalDateTime NOW = LocalDateTime.now(CLOCK);

    private TestData() {
    }

    public static Client client(Long id) {
        return Client.builder()
                .id(id)
                .identificationType(IdentificationType.CC)
                .identificationNumber("1075000001")
                .firstName("Laura")
                .lastName("Gomez")
                .email("laura.gomez@example.com")
                .birthDate(LocalDate.of(1992, 3, 14))
                .createdAt(NOW.minusDays(10))
                .build();
    }

    public static Account account(Long id, AccountType type, String number, String balance) {
        Account account = Account.builder()
                .id(id)
                .type(type)
                .accountNumber(number)
                .status(AccountStatus.ACTIVE)
                .balance(new BigDecimal(balance))
                .gmfExempt(false)
                .createdAt(NOW.minusDays(5))
                .clientId(1L)
                .build();
        account.refreshAvailableBalance();
        return account;
    }

    public static Account savings(Long id, String number, String balance) {
        return account(id, AccountType.SAVINGS, number, balance);
    }

    public static Account checking(Long id, String number, String balance) {
        return account(id, AccountType.CHECKING, number, balance);
    }
}
