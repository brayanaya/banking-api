package com.trinity.banking.domain.port.in;

import java.util.List;

import com.trinity.banking.domain.model.Client;

public interface ClientUseCase {

    Client create(ClientCommand command);

    Client update(Long id, ClientCommand command);

    Client getById(Long id);

    List<Client> getAll();

    void delete(Long id);
}
