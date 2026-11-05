package com.trinity.banking.domain.port.in;

import java.util.List;

import com.trinity.banking.domain.model.Account;
import com.trinity.banking.domain.model.AccountStatus;

public interface AccountUseCase {

    Account create(CreateAccountCommand command);

    Account getById(Long id);

    /** Lista todas las cuentas o, si {@code clientId} no es nulo, las de ese cliente. */
    List<Account> getAll(Long clientId);

    Account changeStatus(Long id, AccountStatus status);
}
