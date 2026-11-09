import React, { useState, useEffect } from 'react';
import { Client, Account, ClientPayload, CreateAccountPayload, AccountStatus } from './types';
import { api } from './services/api';
import { ClientsList } from './components/clients/ClientsList';
import { ClientForm } from './components/clients/ClientForm';
import { AccountsList } from './components/accounts/AccountsList';
import { CreateAccountModal } from './components/accounts/CreateAccountModal';
import { TransactionModal } from './components/transactions/TransactionModal';
import { TransferModal } from './components/transactions/TransferModal';
import { AccountStatementModal } from './components/accounts/AccountStatementModal';
import { Modal, Alert } from './components/common/Modal';
import { Landmark, ShieldCheck, Activity } from 'lucide-react';

export const App: React.FC = () => {
  const [clients, setClients] = useState<Client[]>([]);
  const [selectedClient, setSelectedClient] = useState<Client | null>(null);
  const [clientAccounts, setClientAccounts] = useState<Account[]>([]);
  const [allAccounts, setAllAccounts] = useState<Account[]>([]);

  // Feedback states
  const [loading, setLoading] = useState<boolean>(false);
  const [globalError, setGlobalError] = useState<string | null>(null);
  const [globalSuccess, setGlobalSuccess] = useState<string | null>(null);

  // Modal states
  const [isClientModalOpen, setIsClientModalOpen] = useState<boolean>(false);
  const [editingClient, setEditingClient] = useState<Client | null>(null);

  const [isAccountModalOpen, setIsAccountModalOpen] = useState<boolean>(false);

  const [txModalData, setTxModalData] = useState<{
    account: Account;
    type: 'DEPOSIT' | 'WITHDRAWAL';
  } | null>(null);

  const [transferModalAccount, setTransferModalAccount] = useState<Account | null>(null);
  const [statementModalAccount, setStatementModalAccount] = useState<Account | null>(null);

  // Load clients & all accounts
  const loadInitialData = async () => {
    setLoading(true);
    setGlobalError(null);
    try {
      const [fetchedClients, fetchedAccounts] = await Promise.all([
        api.getClients(),
        api.getAccounts(),
      ]);
      setClients(fetchedClients);
      setAllAccounts(fetchedAccounts);

      if (fetchedClients.length > 0 && !selectedClient) {
        setSelectedClient(fetchedClients[0]);
      }
    } catch (err: unknown) {
      if (err instanceof Error) {
        setGlobalError(err.message);
      } else {
        setGlobalError('Error al conectar con la API Bancaria');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadInitialData();
  }, []);

  // Update client accounts whenever selected client changes
  useEffect(() => {
    if (selectedClient) {
      api
        .getAccounts(selectedClient.id)
        .then((accs) => setClientAccounts(accs))
        .catch((err) => console.error(err));
    } else {
      setClientAccounts([]);
    }
  }, [selectedClient]);

  const refreshAccounts = async () => {
    if (selectedClient) {
      const [accs, allAccs] = await Promise.all([
        api.getAccounts(selectedClient.id),
        api.getAccounts(),
      ]);
      setClientAccounts(accs);
      setAllAccounts(allAccs);
    }
  };

  // CLIENT HANDLERS
  const handleSaveClient = async (payload: ClientPayload) => {
    setLoading(true);
    try {
      if (editingClient) {
        const updated = await api.updateClient(editingClient.id, payload);
        setClients(clients.map((c) => (c.id === updated.id ? updated : c)));
        if (selectedClient?.id === updated.id) setSelectedClient(updated);
        setGlobalSuccess('Cliente actualizado exitosamente');
      } else {
        const created = await api.createClient(payload);
        setClients([created, ...clients]);
        setSelectedClient(created);
        setGlobalSuccess('Cliente creado exitosamente');
      }
      setIsClientModalOpen(false);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteClient = async (client: Client) => {
    if (!window.confirm(`¿Estás seguro de eliminar al cliente ${client.firstName} ${client.lastName}?`)) return;

    setLoading(true);
    setGlobalError(null);
    try {
      await api.deleteClient(client.id);
      setClients(clients.filter((c) => c.id !== client.id));
      if (selectedClient?.id === client.id) {
        setSelectedClient(clients.find((c) => c.id !== client.id) || null);
      }
      setGlobalSuccess('Cliente eliminado con éxito');
    } catch (err: unknown) {
      if (err instanceof Error) {
        setGlobalError(err.message);
      }
    } finally {
      setLoading(false);
    }
  };

  // ACCOUNT HANDLERS
  const handleCreateAccount = async (payload: CreateAccountPayload) => {
    setLoading(true);
    try {
      await api.createAccount(payload);
      await refreshAccounts();
      setGlobalSuccess('Producto financiero creado exitosamente');
    } finally {
      setLoading(false);
    }
  };

  const handleChangeAccountStatus = async (account: Account, status: AccountStatus) => {
    const actionName = status === 'CANCELLED' ? 'cancelar' : status === 'INACTIVE' ? 'inactivar' : 'activar';
    if (!window.confirm(`¿Deseas ${actionName} la cuenta ${account.accountNumber}?`)) return;

    setLoading(true);
    setGlobalError(null);
    try {
      await api.changeAccountStatus(account.id, status);
      await refreshAccounts();
      setGlobalSuccess(`Cuenta ${account.accountNumber} actualizada a estado ${status}`);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setGlobalError(err.message);
      }
    } finally {
      setLoading(false);
    }
  };

  // TRANSACTION HANDLERS
  const handleCashMovement = async (accountNumber: string, amount: number, description?: string) => {
    setLoading(true);
    try {
      if (txModalData?.type === 'DEPOSIT') {
        await api.deposit(accountNumber, amount, description);
        setGlobalSuccess(`Consignación de $${amount.toLocaleString('es-CO')} realizada con éxito`);
      } else {
        await api.withdraw(accountNumber, amount, description);
        setGlobalSuccess(`Retiro de $${amount.toLocaleString('es-CO')} realizado con éxito`);
      }
      await refreshAccounts();
    } finally {
      setLoading(false);
    }
  };

  const handleTransfer = async (
    originNumber: string,
    destinationNumber: string,
    amount: number,
    description?: string
  ) => {
    setLoading(true);
    try {
      await api.transfer(originNumber, destinationNumber, amount, description);
      await refreshAccounts();
      setGlobalSuccess(`Transferencia de $${amount.toLocaleString('es-CO')} efectuada correctamente`);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      {/* Header */}
      <header className="bg-slate-900 text-white border-b border-slate-800 shadow-md sticky top-0 z-40">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-blue-600 rounded-lg shadow-sm">
              <Landmark className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-lg font-bold tracking-tight">Trinity Banking System</h1>
              <p className="text-[11px] text-slate-400">Portal de Administración para Empleados Bancarios</p>
            </div>
          </div>

          <div className="flex items-center gap-6 text-xs text-slate-300">
            <div className="flex items-center gap-1.5">
              <ShieldCheck className="w-4 h-4 text-emerald-400" />
              <span>Arquitectura Hexagonal</span>
            </div>
            <div className="flex items-center gap-1.5">
              <Activity className="w-4 h-4 text-blue-400" />
              <span>PostgreSQL + ACID</span>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 flex-1 w-full">
        <Alert message={globalError} type="error" onClose={() => setGlobalError(null)} />
        <Alert message={globalSuccess} type="success" onClose={() => setGlobalSuccess(null)} />

        {/* Clients Table Component */}
        <ClientsList
          clients={clients}
          selectedClient={selectedClient}
          onSelectClient={(client) => setSelectedClient(client)}
          onAddClient={() => {
            setEditingClient(null);
            setIsClientModalOpen(true);
          }}
          onEditClient={(client) => {
            setEditingClient(client);
            setIsClientModalOpen(true);
          }}
          onDeleteClient={handleDeleteClient}
        />

        {/* Selected Client Accounts Component */}
        {selectedClient && (
          <AccountsList
            client={selectedClient}
            accounts={clientAccounts}
            onOpenCreateModal={() => setIsAccountModalOpen(true)}
            onChangeStatus={handleChangeAccountStatus}
            onOpenTransactionModal={(account, type) => setTxModalData({ account, type })}
            onOpenTransferModal={(account) => setTransferModalAccount(account)}
            onOpenStatementModal={(account) => setStatementModalAccount(account)}
          />
        )}
      </main>

      {/* MODALS */}
      {/* Client Create/Edit Modal */}
      <Modal
        isOpen={isClientModalOpen}
        onClose={() => setIsClientModalOpen(false)}
        title={editingClient ? 'Modificar Información del Cliente' : 'Registro de Nuevo Cliente'}
      >
        <ClientForm
          initialData={editingClient}
          onSubmit={handleSaveClient}
          onCancel={() => setIsClientModalOpen(false)}
          isLoading={loading}
        />
      </Modal>

      {/* Create Account Modal */}
      {selectedClient && (
        <Modal
          isOpen={isAccountModalOpen}
          onClose={() => setIsAccountModalOpen(false)}
          title="Crear Nuevo Producto Financiero"
        >
          <CreateAccountModal
            clientId={selectedClient.id}
            clientName={`${selectedClient.firstName} ${selectedClient.lastName}`}
            onSubmit={handleCreateAccount}
            onClose={() => setIsAccountModalOpen(false)}
            isLoading={loading}
          />
        </Modal>
      )}

      {/* Cash Movement (Deposit/Withdrawal) Modal */}
      {txModalData && (
        <Modal
          isOpen={!!txModalData}
          onClose={() => setTxModalData(null)}
          title={txModalData.type === 'DEPOSIT' ? 'Realizar Consignación' : 'Realizar Retiro'}
        >
          <TransactionModal
            account={txModalData.account}
            type={txModalData.type}
            onClose={() => setTxModalData(null)}
            onSubmit={handleCashMovement}
            isLoading={loading}
          />
        </Modal>
      )}

      {/* Transfer Modal */}
      {transferModalAccount && (
        <Modal
          isOpen={!!transferModalAccount}
          onClose={() => setTransferModalAccount(null)}
          title="Transferencia entre Cuentas"
        >
          <TransferModal
            originAccount={transferModalAccount}
            allAccounts={allAccounts}
            onClose={() => setTransferModalAccount(null)}
            onSubmit={handleTransfer}
            isLoading={loading}
          />
        </Modal>
      )}

      {/* Statement Modal */}
      {statementModalAccount && (
        <Modal
          isOpen={!!statementModalAccount}
          onClose={() => setStatementModalAccount(null)}
          title={`Estado de Cuenta - ${statementModalAccount.accountNumber}`}
        >
          <AccountStatementModal
            account={statementModalAccount}
            onClose={() => setStatementModalAccount(null)}
          />
        </Modal>
      )}

      {/* Footer */}
      <footer className="bg-white border-t border-slate-200 py-4 text-center text-xs text-slate-500">
        Trinity Banking Portal &copy; {new Date().getFullYear()} - Sistema bancario institucional
      </footer>
    </div>
  );
};