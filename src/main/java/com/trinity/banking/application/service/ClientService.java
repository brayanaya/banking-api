package com.trinity.banking.application.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.trinity.banking.domain.exception.BusinessRuleException;
import com.trinity.banking.domain.exception.DuplicateResourceException;
import com.trinity.banking.domain.exception.ResourceNotFoundException;
import com.trinity.banking.domain.model.Client;
import com.trinity.banking.domain.port.in.ClientCommand;
import com.trinity.banking.domain.port.in.ClientUseCase;
import com.trinity.banking.domain.port.out.AccountRepositoryPort;
import com.trinity.banking.domain.port.out.ClientRepositoryPort;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientService implements ClientUseCase {

    private static final int MIN_NAME_LENGTH = 2;

    private final ClientRepositoryPort clientRepository;
    private final AccountRepositoryPort accountRepository;
    private final Clock clock;

    @Override
    public Client create(ClientCommand command) {
        assertAdult(command);
        Client client = Client.builder()
                .identificationType(command.identificationType())
                .identificationNumber(normalizeIdentification(command.identificationNumber()))
                .firstName(normalizeName(command.firstName(), "nombres"))
                .lastName(normalizeName(command.lastName(), "apellido"))
                .email(normalizeEmail(command.email()))
                .birthDate(command.birthDate())
                .createdAt(LocalDateTime.now(clock))   // fecha de creación automática
                .build();
        assertUnique(client, null);
        return clientRepository.save(client);
    }

    @Override
    public Client update(Long id, ClientCommand command) {
        Client client = findOrFail(id);
        assertAdult(command);

        client.setIdentificationType(command.identificationType());
        client.setIdentificationNumber(normalizeIdentification(command.identificationNumber()));
        client.setFirstName(normalizeName(command.firstName(), "nombres"));
        client.setLastName(normalizeName(command.lastName(), "apellido"));
        client.setEmail(normalizeEmail(command.email()));
        client.setBirthDate(command.birthDate());
        client.setUpdatedAt(LocalDateTime.now(clock)); // fecha de modificación automática

        assertUnique(client, id);
        return clientRepository.save(client);
    }

    @Override
    @Transactional(readOnly = true)
    public Client getById(Long id) {
        return findOrFail(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Client> getAll() {
        return clientRepository.findAll();
    }

    @Override
    public void delete(Long id) {
        findOrFail(id);
        if (accountRepository.existsByClientId(id)) {
            throw new BusinessRuleException("No se puede eliminar un cliente que tiene productos vinculados");
        }
        clientRepository.deleteById(id);
    }

    // ------------------------------------------------------------------ helpers

    private Client findOrFail(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente con id " + id + " no encontrado"));
    }

    private void assertAdult(ClientCommand command) {
        if (!Client.isAdult(command.birthDate(), LocalDate.now(clock))) {
            throw new BusinessRuleException(
                    "El cliente debe ser mayor de edad (" + Client.MINIMUM_AGE + " años o más)");
        }
    }

    private void assertUnique(Client client, Long ignoreId) {
        clientRepository.findByIdentification(client.getIdentificationType(), client.getIdentificationNumber())
                .filter(existing -> !existing.getId().equals(ignoreId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Ya existe un cliente con la identificación "
                            + client.getIdentificationType() + " " + client.getIdentificationNumber());
                });
        clientRepository.findByEmail(client.getEmail())
                .filter(existing -> !existing.getId().equals(ignoreId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Ya existe un cliente con el correo " + client.getEmail());
                });
    }

    private static String normalizeName(String value, String field) {
        String name = value == null ? "" : value.strip();
        if (name.length() < MIN_NAME_LENGTH) {
            throw new BusinessRuleException(
                    "El campo " + field + " debe tener al menos " + MIN_NAME_LENGTH + " caracteres");
        }
        return name;
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    private static String normalizeIdentification(String number) {
        return number == null ? null : number.strip().toUpperCase(Locale.ROOT);
    }
}
