package com.trinity.banking.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.trinity.banking.domain.exception.BusinessRuleException;
import com.trinity.banking.domain.model.Account;
import com.trinity.banking.domain.model.AccountType;
import com.trinity.banking.domain.model.Client;
import com.trinity.banking.domain.model.IdentificationType;
import com.trinity.banking.domain.port.in.ClientCommand;
import com.trinity.banking.domain.port.in.ClientUseCase;
import com.trinity.banking.domain.port.in.CreateAccountCommand;
import com.trinity.banking.domain.port.in.TransactionUseCase;
import com.trinity.banking.domain.port.out.AccountRepositoryPort;

@Testcontainers
@SpringBootTest
class DatabaseIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("banking")
            .withUsername("banking")
            .withPassword("banking");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    ClientUseCase clientUseCase;

    @Autowired
    AccountRepositoryPort accountRepository;

    @Autowired
    TransactionUseCase transactionUseCase;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void waitForContainer() {
        // El contenedor ya está iniciado por @Container
    }

    @AfterAll
    static void stopContainer() {
        // El contenedor se detiene automáticamente
    }

    // (a) El trigger rechaza un cliente menor de edad por SQL directo
    @Test
    void triggerRejectsMinorInsertedDirectlyInDatabase() {
        String sql = """
            INSERT INTO clients (identification_type, identification_number, first_name, last_name, email, birth_date, created_at)
            VALUES ('CC', '9999999999', 'Nino', 'Menor', 'nino@test.co', '2015-01-01', NOW())
            """;

        assertThatThrownBy(() -> jdbcTemplate.execute(sql))
                .hasCauseInstanceOf(SQLException.class)
                .cause()
                .hasMessageContaining("El cliente debe ser mayor de edad");
    }

    // (b) El CHECK de ahorros rechaza saldo negativo
    @Test
    void checkConstraintRejectsNegativeSavingsBalance() {
        // Crear cliente válido
        Client client = clientUseCase.create(new ClientCommand(
                IdentificationType.CC, "1075999999", "Test", "User", "test@domain.co",
                LocalDate.of(1990, 1, 1)));

        // Intentar insertar cuenta de ahorros con saldo negativo directamente en BD
        String sql = """
            INSERT INTO accounts (account_type, account_number, status, balance, available_balance,
                                  gmf_exempt, created_at, client_id)
            VALUES ('SAVINGS', '5300009999', 'ACTIVE', -100.00, -100.00, FALSE, NOW(), ?)
            """;

        assertThatThrownBy(() -> jdbcTemplate.update(sql, client.getId()))
                .hasCauseInstanceOf(SQLException.class)
                .cause()
                .hasMessageContaining("ck_accounts_savings_balance");
    }

    // (c) La FK impide borrar un cliente con cuentas
    @Test
    void foreignKeyPreventsDeletingClientWithAccounts() {
        // Crear cliente con identificación y correo únicos para aislar datos
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        Client client = clientUseCase.create(new ClientCommand(
                IdentificationType.CC, "1075888" + uniqueId, "Test", "User", "test-" + uniqueId + "@domain.co",
                LocalDate.of(1990, 1, 1)));

        // Crear cuenta para ese cliente (llamar refreshAvailableBalance para inicializar available_balance)
        Account account = Account.builder()
                .type(AccountType.SAVINGS)
                .accountNumber("5300008888")
                .status(com.trinity.banking.domain.model.AccountStatus.ACTIVE)
                .balance(BigDecimal.ZERO)
                .gmfExempt(false)
                .createdAt(java.time.LocalDateTime.now())
                .clientId(client.getId())
                .build();
        account.refreshAvailableBalance();
        accountRepository.save(account);

        // Intentar borrar el cliente directamente en BD
        String sql = "DELETE FROM clients WHERE id = ?";

        assertThatThrownBy(() -> jdbcTemplate.update(sql, client.getId()))
                .hasCauseInstanceOf(SQLException.class)
                .cause()
                .hasMessageContaining("fk_accounts_client");
    }

    // (d) El CHECK de email rechaza a@b.c (TLD 1 char)
    @Test
    void checkConstraintRejectsOneCharTldEmail() {
        String sql = """
            INSERT INTO clients (identification_type, identification_number, first_name, last_name, email, birth_date, created_at)
            VALUES ('CC', '9999999998', 'Test', 'User', 'a@b.c', '1990-01-01', NOW())
            """;

        assertThatThrownBy(() -> jdbcTemplate.execute(sql))
                .hasCauseInstanceOf(SQLException.class)
                .cause()
                .hasMessageContaining("ck_clients_email_format");
    }

    // (e) Transferencia concurrente cruzada A→B y B→A termina sin deadlock y saldos suman lo mismo
    @Test
    void concurrentCrossTransfersCompleteWithoutDeadlockAndPreserveTotalBalance() throws InterruptedException {
        // Crear dos clientes
        Client clientA = clientUseCase.create(new ClientCommand(
                IdentificationType.CC, "1075111111", "Alice", "User", "alice@test.co",
                LocalDate.of(1990, 1, 1)));

        Client clientB = clientUseCase.create(new ClientCommand(
                IdentificationType.CC, "1075222222", "Bob", "User", "bob@test.co",
                LocalDate.of(1990, 1, 1)));

        // Crear cuentas de ahorros para ambos con saldo inicial
        Account accountA = Account.builder()
                .type(AccountType.SAVINGS)
                .accountNumber("5300001111")
                .status(com.trinity.banking.domain.model.AccountStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .gmfExempt(false)
                .createdAt(java.time.LocalDateTime.now())
                .clientId(clientA.getId())
                .build();
        accountA.refreshAvailableBalance();
        Account savedAccountA = accountRepository.save(accountA);

        Account accountB = Account.builder()
                .type(AccountType.SAVINGS)
                .accountNumber("5300002222")
                .status(com.trinity.banking.domain.model.AccountStatus.ACTIVE)
                .balance(new BigDecimal("1000.00"))
                .gmfExempt(false)
                .createdAt(java.time.LocalDateTime.now())
                .clientId(clientB.getId())
                .build();
        accountB.refreshAvailableBalance();
        Account savedAccountB = accountRepository.save(accountB);

        // Registrar movimiento inicial para que el estado de cuenta cuadre
        jdbcTemplate.update("""
            INSERT INTO transactions (reference, transaction_type, movement_type, amount, balance_after,
                                      available_balance_after, account_id, account_number, description, created_at)
            VALUES (gen_random_uuid()::text, 'DEPOSIT', 'CREDIT', ?, ?, ?, ?, ?, 'Saldo inicial', NOW())
            """, savedAccountA.getBalance(), savedAccountA.getBalance(), savedAccountA.getAvailableBalance(), savedAccountA.getId(), savedAccountA.getAccountNumber());

        jdbcTemplate.update("""
            INSERT INTO transactions (reference, transaction_type, movement_type, amount, balance_after,
                                      available_balance_after, account_id, account_number, description, created_at)
            VALUES (gen_random_uuid()::text, 'DEPOSIT', 'CREDIT', ?, ?, ?, ?, ?, 'Saldo inicial', NOW())
            """, savedAccountB.getBalance(), savedAccountB.getBalance(), savedAccountB.getAvailableBalance(), savedAccountB.getId(), savedAccountB.getAccountNumber());

        // Ejecutar 2 hilos: A→B (100) y B→A (50) concurrentemente
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicReference<Throwable> errorA = new AtomicReference<>();
        AtomicReference<Throwable> errorB = new AtomicReference<>();

        final String accountNumberA = savedAccountA.getAccountNumber();
        final String accountNumberB = savedAccountB.getAccountNumber();
        final Long accountIdA = savedAccountA.getId();
        final Long accountIdB = savedAccountB.getId();

        executor.submit(() -> {
            try {
                startLatch.await();
                transactionUseCase.transfer(accountNumberA, accountNumberB,
                        new BigDecimal("100.00"), "A to B");
            } catch (Throwable t) {
                errorA.set(t);
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                transactionUseCase.transfer(accountNumberB, accountNumberA,
                        new BigDecimal("50.00"), "B to A");
            } catch (Throwable t) {
                errorB.set(t);
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        assertThat(doneLatch.await(30, TimeUnit.SECONDS)).isTrue();
        executor.shutdown();

        // Verificar que no hubo errores
        assertThat(errorA.get()).isNull();
        assertThat(errorB.get()).isNull();

        // Verificar saldos finales: A = 1000 - 100 + 50 = 950, B = 1000 + 100 - 50 = 1050
        Account finalA = accountRepository.findById(accountIdA).orElseThrow();
        Account finalB = accountRepository.findById(accountIdB).orElseThrow();

        assertThat(finalA.getBalance()).isEqualByComparingTo("950.00");
        assertThat(finalB.getBalance()).isEqualByComparingTo("1050.00");

        // La suma total debe ser igual a la inicial (2000)
        BigDecimal total = finalA.getBalance().add(finalB.getBalance());
        assertThat(total).isEqualByComparingTo("2000.00");
    }
}