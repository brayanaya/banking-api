package com.trinity.banking.infrastructure.adapter.in.web;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.trinity.banking.domain.model.Client;
import com.trinity.banking.domain.port.in.ClientUseCase;
import com.trinity.banking.infrastructure.adapter.in.web.dto.ClientRequest;
import com.trinity.banking.infrastructure.adapter.in.web.dto.ClientResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Clientes")
public class ClientController {

    private final ClientUseCase clientUseCase;

    @PostMapping
    @Operation(summary = "Crear un cliente (solo mayores de edad)")
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientRequest request) {
        Client created = clientUseCase.create(request.toCommand());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(ClientResponse.from(created));
    }

    @GetMapping
    @Operation(summary = "Listar clientes")
    public List<ClientResponse> getAll() {
        return clientUseCase.getAll().stream().map(ClientResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar un cliente por id")
    public ClientResponse getById(@PathVariable Long id) {
        return ClientResponse.from(clientUseCase.getById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un cliente (recalcula la fecha de modificación)")
    public ClientResponse update(@PathVariable Long id, @Valid @RequestBody ClientRequest request) {
        return ClientResponse.from(clientUseCase.update(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un cliente (no permitido si tiene productos vinculados)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        clientUseCase.delete(id);
        return ResponseEntity.noContent().build();
    }
}
