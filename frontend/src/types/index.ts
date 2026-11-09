export type IdentificationType = 'CC' | 'CE' | 'PASSPORT' | 'NIT';
export type AccountType = 'SAVINGS' | 'CHECKING';
export type AccountStatus = 'ACTIVE' | 'INACTIVE' | 'CANCELLED';
export type TransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER';
export type MovementType = 'DEBIT' | 'CREDIT';

export interface Client {
  id: number;
  identificationType: IdentificationType;
  identificationNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  birthDate: string;
  createdAt: string;
  updatedAt?: string | null;
}

export interface ClientPayload {
  identificationType: IdentificationType;
  identificationNumber: string;
  firstName: string;
  lastName: string;
  email: string;
  birthDate: string;
}

export interface Account {
  id: number;
  type: AccountType;
  accountNumber: string;
  status: AccountStatus;
  balance: number;
  availableBalance: number;
  gmfExempt: boolean;
  createdAt: string;
  updatedAt?: string | null;
  clientId: number;
}

export interface CreateAccountPayload {
  clientId: number;
  type: AccountType;
  initialBalance: number;
  gmfExempt?: boolean;
}

export interface Movement {
  id: number;
  reference: string;
  transactionType: TransactionType;
  movementType: MovementType;
  amount: number;
  balanceAfter: number;
  availableBalanceAfter: number;
  accountNumber: string;
  relatedAccountNumber?: string | null;
  description?: string;
  createdAt: string;
}

export interface AccountStatement {
  account: Account;
  from: string;
  to: string;
  totalCredits: number;
  totalDebits: number;
  movements: Movement[];
}

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  details?: string[];
}