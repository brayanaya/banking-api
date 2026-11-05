package com.trinity.banking.infrastructure.adapter.in.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.trinity.banking.domain.port.in.TransactionUseCase;
import com.trinity.banking.infrastructure.adapter.in.web.dto.CashMovementRequest;
import com.trinity.banking.infrastructure.adapter.in.web.dto.TransactionResponse;
import com.trinity.banking.infrastructure.adapter.in.web.dto.TransferRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transacciones")
public class TransactionController {

    private final TransactionUseCase transactionUseCase;

    @PostMapping("/deposits")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Consignación")
    public TransactionResponse deposit(@Valid @RequestBody CashMovementRequest request) {
        return TransactionResponse.from(transactionUseCase.deposit(
                request.accountNumber(), request.amount(), request.description()));
    }

    @PostMapping("/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Retiro")
    public TransactionResponse withdraw(@Valid @RequestBody CashMovementRequest request) {
        return TransactionResponse.from(transactionUseCase.withdraw(
                request.accountNumber(), request.amount(), request.description()));
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transferencia entre cuentas (genera débito en origen y crédito en destino)")
    public List<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        return transactionUseCase.transfer(request.originAccountNumber(), request.destinationAccountNumber(),
                        request.amount(), request.description())
                .stream().map(TransactionResponse::from).toList();
    }
}
