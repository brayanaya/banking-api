import { Client, ClientPayload, Account, CreateAccountPayload, AccountStatement, Movement } from '../types';

const BASE_URL = '/api/v1';

async function handleResponse<T>(res: Response): Promise<T> {
  if (!res.ok) {
    let errorData;
    try {
      errorData = await res.json();
    } catch {
      throw new Error(`Error ${res.status}: ${res.statusText}`);
    }
    const message = errorData.message || (errorData.details && errorData.details.join(', ')) || 'Error en la solicitud';
    throw new Error(message);
  }
  if (res.status === 204) {
    return {} as T;
  }
  return res.json();
}

export const api = {
  // CLIENTS
  async getClients(): Promise<Client[]> {
    const res = await fetch(`${BASE_URL}/clients`);
    return handleResponse<Client[]>(res);
  },

  async getClient(id: number): Promise<Client> {
    const res = await fetch(`${BASE_URL}/clients/${id}`);
    return handleResponse<Client>(res);
  },

  async createClient(payload: ClientPayload): Promise<Client> {
    const res = await fetch(`${BASE_URL}/clients`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return handleResponse<Client>(res);
  },

  async updateClient(id: number, payload: ClientPayload): Promise<Client> {
    const res = await fetch(`${BASE_URL}/clients/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return handleResponse<Client>(res);
  },

  async deleteClient(id: number): Promise<void> {
    const res = await fetch(`${BASE_URL}/clients/${id}`, { method: 'DELETE' });
    return handleResponse<void>(res);
  },

  // ACCOUNTS
  async getAccounts(clientId?: number): Promise<Account[]> {
    const url = clientId ? `${BASE_URL}/accounts?clientId=${clientId}` : `${BASE_URL}/accounts`;
    const res = await fetch(url);
    return handleResponse<Account[]>(res);
  },

  async getAccount(id: number): Promise<Account> {
    const res = await fetch(`${BASE_URL}/accounts/${id}`);
    return handleResponse<Account>(res);
  },

  async createAccount(payload: CreateAccountPayload): Promise<Account> {
    const res = await fetch(`${BASE_URL}/accounts`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
    });
    return handleResponse<Account>(res);
  },

  async changeAccountStatus(id: number, status: 'ACTIVE' | 'INACTIVE' | 'CANCELLED'): Promise<Account> {
    const res = await fetch(`${BASE_URL}/accounts/${id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    });
    return handleResponse<Account>(res);
  },

  async getStatement(id: number, from?: string, to?: string): Promise<AccountStatement> {
    let url = `${BASE_URL}/accounts/${id}/statement`;
    const params = new URLSearchParams();
    if (from) params.append('from', from);
    if (to) params.append('to', to);
    if (params.toString()) url += `?${params.toString()}`;
    const res = await fetch(url);
    return handleResponse<AccountStatement>(res);
  },

  // TRANSACTIONS
  async deposit(accountNumber: string, amount: number, description?: string): Promise<Movement> {
    const res = await fetch(`${BASE_URL}/transactions/deposits`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ accountNumber, amount, description }),
    });
    return handleResponse<Movement>(res);
  },

  async withdraw(accountNumber: string, amount: number, description?: string): Promise<Movement> {
    const res = await fetch(`${BASE_URL}/transactions/withdrawals`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ accountNumber, amount, description }),
    });
    return handleResponse<Movement>(res);
  },

  async transfer(originAccountNumber: string, destinationAccountNumber: string, amount: number, description?: string): Promise<Movement[]> {
    const res = await fetch(`${BASE_URL}/transactions/transfers`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ originAccountNumber, destinationAccountNumber, amount, description }),
    });
    return handleResponse<Movement[]>(res);
  },
};