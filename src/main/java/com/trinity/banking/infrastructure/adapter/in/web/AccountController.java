package com.trinity.banking.infrastructure.adapter.in.web;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.trinity.banking.domain.model.Account;
import com.trinity.banking.domain.port.in.AccountStatementUseCase;
import com.trinity.banking.domain.port.in.AccountUseCase;
import com.trinity.banking.infrastructure.adapter.in.web.dto.AccountResponse;
import com.trinity.banking.infrastructure.adapter.in.web.dto.AccountStatementResponse;
import com.trinity.banking.infrastructure.adapter.in.web.dto.CreateAccountRequest;
import com.trinity.banking.infrastructure.adapter.in.web.dto.UpdateAccountStatusRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Productos (cuentas)")
public class AccountController {

    private final AccountUseCase accountUseCase;
    private final AccountStatementUseCase statementUseCase;

    @PostMapping
    @Operation(summary = "Crear una cuenta corriente (33...) o de ahorros (53...) para un cliente")
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        Account created = accountUseCase.create(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(AccountResponse.from(created));
    }

    @GetMapping
    @Operation(summary = "Listar cuentas (opcionalmente filtradas por cliente)")
    public List<AccountResponse> getAll(@RequestParam(required = false) Long clientId) {
        return accountUseCase.getAll(clientId).stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una cuenta por id")
    public AccountResponse getById(@PathVariable Long id) {
        return AccountResponse.from(accountUseCase.getById(id));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activar, inactivar o cancelar una cuenta (cancelar exige saldo $0)")
    public AccountResponse changeStatus(@PathVariable Long id,
                                        @Valid @RequestBody UpdateAccountStatusRequest request) {
        return AccountResponse.from(accountUseCase.changeStatus(id, request.status()));
    }

    @GetMapping("/{id}/statement")
    @Operation(summary = "Estado de cuenta (por defecto, el mes en curso)")
    public AccountStatementResponse statement(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return AccountStatementResponse.from(statementUseCase.getStatement(id, from, to));
    }
}
