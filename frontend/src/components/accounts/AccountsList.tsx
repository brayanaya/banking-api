import React, { useState } from 'react';
import { Account, AccountStatus, Client } from '../../types';
import { PlusCircle, Power, Ban, RefreshCw, ArrowRightLeft, FileSpreadsheet } from 'lucide-react';

interface AccountsListProps {
  client: Client;
  accounts: Account[];
  onOpenCreateModal: () => void;
  onChangeStatus: (account: Account, status: AccountStatus) => void;
  onOpenTransactionModal: (account: Account, type: 'DEPOSIT' | 'WITHDRAWAL') => void;
  onOpenTransferModal: (originAccount: Account) => void;
  onOpenStatementModal: (account: Account) => void;
}

export const AccountsList: React.FC<AccountsListProps> = ({
  client,
  accounts,
  onOpenCreateModal,
  onChangeStatus,
  onOpenTransactionModal,
  onOpenTransferModal,
  onOpenStatementModal,
}) => {
  const [filterType, setFilterType] = useState<string>('ALL');

  const filteredAccounts = accounts.filter((acc) => {
    if (filterType === 'SAVINGS') return acc.type === 'SAVINGS';
    if (filterType === 'CHECKING') return acc.type === 'CHECKING';
    return true;
  });

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      minimumFractionDigits: 2,
    }).format(val);
  };

  const getStatusBadge = (status: AccountStatus) => {
    switch (status) {
      case 'ACTIVE':
        return <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">Activa</span>;
      case 'INACTIVE':
        return <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800">Inactiva</span>;
      case 'CANCELLED':
        return <span className="px-2 py-0.5 rounded-full text-xs font-semibold bg-rose-100 text-rose-800">Cancelada</span>;
    }
  };

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden mt-6">
      <div className="p-5 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-slate-50/50">
        <div>
          <div className="flex items-center gap-2">
            <h3 className="text-base font-bold text-slate-800">
              Productos Financieros de {client.firstName} {client.lastName}
            </h3>
            <span className="text-xs bg-blue-100 text-blue-700 font-semibold px-2 py-0.5 rounded">
              {client.identificationType}: {client.identificationNumber}
            </span>
          </div>
          <p className="text-xs text-slate-500 mt-0.5">
            Cuentas de Ahorros (53...) y Cuentas Corrientes (33...)
          </p>
        </div>

        <div className="flex items-center gap-3">
          <select
            value={filterType}
            onChange={(e) => setFilterType(e.target.value)}
            className="px-3 py-1.5 border rounded-lg text-xs bg-white focus:outline-none"
          >
            <option value="ALL">Todos los tipos</option>
            <option value="SAVINGS">Cuentas de Ahorro</option>
            <option value="CHECKING">Cuentas Corrientes</option>
          </select>
          <button
            onClick={onOpenCreateModal}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600 text-white rounded-lg text-sm font-semibold hover:bg-emerald-700 transition shadow-sm"
          >
            <PlusCircle className="w-4 h-4" />
            <span>Crear Producto</span>
          </button>
        </div>
      </div>

      <div className="p-5">
        {filteredAccounts.length === 0 ? (
          <div className="text-center py-10 text-slate-400 bg-slate-50/50 rounded-lg border border-dashed">
            Este cliente aún no tiene productos financieros registrados.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {filteredAccounts.map((acc) => (
              <div
                key={acc.id}
                className={`p-4 rounded-xl border transition shadow-sm flex flex-col justify-between ${
                  acc.status === 'CANCELLED'
                    ? 'bg-slate-50 border-slate-200 opacity-75'
                    : 'bg-white border-slate-200 hover:border-blue-400'
                }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-xs font-bold uppercase tracking-wider text-slate-500">
                      {acc.type === 'SAVINGS' ? 'Cuenta de Ahorros' : 'Cuenta Corriente'}
                    </span>
                    {getStatusBadge(acc.status)}
                  </div>

                  <div className="text-xl font-mono font-bold text-slate-800 tracking-wide mb-3">
                    {acc.accountNumber}
                  </div>

                  <div className="bg-slate-50 p-2.5 rounded-lg border border-slate-100 mb-3 space-y-1">
                    <div className="flex justify-between text-xs">
                      <span className="text-slate-500">Saldo Real:</span>
                      <span className={`font-semibold ${acc.balance < 0 ? 'text-red-600' : 'text-slate-800'}`}>
                        {formatCurrency(acc.balance)}
                      </span>
                    </div>
                    <div className="flex justify-between text-xs">
                      <span className="text-slate-500">Disponible:</span>
                      <span className="font-semibold text-emerald-700">
                        {formatCurrency(acc.availableBalance)}
                      </span>
                    </div>
                    <div className="flex justify-between text-[11px] text-slate-400 pt-1 border-t border-slate-200">
                      <span>Exenta 4x1000 (GMF):</span>
                      <span>{acc.gmfExempt ? 'SÍ' : 'NO'}</span>
                    </div>
                  </div>
                </div>

                <div className="space-y-2 pt-2 border-t border-slate-100">
                  {acc.status === 'ACTIVE' && (
                    <div className="grid grid-cols-3 gap-1 text-xs">
                      <button
                        onClick={() => onOpenTransactionModal(acc, 'DEPOSIT')}
                        className="py-1 px-2 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 font-medium rounded border border-emerald-200 text-center transition"
                      >
                        Consignar
                      </button>
                      <button
                        onClick={() => onOpenTransactionModal(acc, 'WITHDRAWAL')}
                        className="py-1 px-2 bg-rose-50 text-rose-700 hover:bg-rose-100 font-medium rounded border border-rose-200 text-center transition"
                      >
                        Retirar
                      </button>
                      <button
                        onClick={() => onOpenTransferModal(acc)}
                        className="py-1 px-2 bg-blue-50 text-blue-700 hover:bg-blue-100 font-medium rounded border border-blue-200 text-center flex items-center justify-center gap-1 transition"
                      >
                        <ArrowRightLeft className="w-3 h-3" />
                        Transf.
                      </button>
                    </div>
                  )}

                  <div className="flex items-center justify-between pt-1 text-xs text-slate-500">
                    <button
                      onClick={() => onOpenStatementModal(acc)}
                      className="flex items-center gap-1 text-blue-600 hover:text-blue-800 font-medium"
                    >
                      <FileSpreadsheet className="w-3.5 h-3.5" />
                      <span>Extracto</span>
                    </button>

                    <div className="flex items-center gap-1.5">
                      {acc.status === 'ACTIVE' && (
                        <button
                          title="Inactivar Cuenta"
                          onClick={() => onChangeStatus(acc, 'INACTIVE')}
                          className="p-1 text-amber-600 hover:bg-amber-50 rounded"
                        >
                          <Power className="w-4 h-4" />
                        </button>
                      )}
                      {acc.status === 'INACTIVE' && (
                        <button
                          title="Activar Cuenta"
                          onClick={() => onChangeStatus(acc, 'ACTIVE')}
                          className="p-1 text-emerald-600 hover:bg-emerald-50 rounded"
                        >
                          <RefreshCw className="w-4 h-4" />
                        </button>
                      )}
                      {acc.status !== 'CANCELLED' && (
                        <button
                          title="Cancelar Cuenta (Exige saldo $0)"
                          onClick={() => onChangeStatus(acc, 'CANCELLED')}
                          className="p-1 text-rose-600 hover:bg-rose-50 rounded"
                        >
                          <Ban className="w-4 h-4" />
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};