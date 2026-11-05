package com.trinity.banking.infrastructure.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.trinity.banking.domain.model.Client;
import com.trinity.banking.domain.model.IdentificationType;
import com.trinity.banking.domain.port.out.ClientRepositoryPort;
import com.trinity.banking.infrastructure.adapter.out.persistence.repository.ClientJpaRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ClientPersistenceAdapter implements ClientRepositoryPort {

    private final ClientJpaRepository jpa;

    @Override
    public Client save(Client client) {
        return PersistenceMapper.toDomain(jpa.save(PersistenceMapper.toEntity(client)));
    }

    @Override
    public Optional<Client> findById(Long id) {
        return jpa.findById(id).map(PersistenceMapper::toDomain);
    }

    @Override
    public List<Client> findAll() {
        return jpa.findAll().stream().map(PersistenceMapper::toDomain).toList();
    }

    @Override
    public Optional<Client> findByIdentification(IdentificationType type, String number) {
        return jpa.findByIdentificationTypeAndIdentificationNumber(type, number).map(PersistenceMapper::toDomain);
    }

    @Override
    public Optional<Client> findByEmail(String email) {
        return jpa.findByEmail(email).map(PersistenceMapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }
}
