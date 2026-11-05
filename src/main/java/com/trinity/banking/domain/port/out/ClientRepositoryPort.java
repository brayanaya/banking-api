package com.trinity.banking.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.trinity.banking.domain.model.Client;
import com.trinity.banking.domain.model.IdentificationType;

public interface ClientRepositoryPort {

    Client save(Client client);

    Optional<Client> findById(Long id);

    List<Client> findAll();

    Optional<Client> findByIdentification(IdentificationType type, String number);

    Optional<Client> findByEmail(String email);

    void deleteById(Long id);
}
