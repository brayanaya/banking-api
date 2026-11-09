import React, { useState } from 'react';
import { Account } from '../../types';

interface TransactionModalProps {
  account: Account;
  type: 'DEPOSIT' | 'WITHDRAWAL';
  onClose: () => void;
  onSubmit: (accountNumber: string, amount: number, description?: string) => Promise<void>;
  isLoading: boolean;
}

export const TransactionModal: React.FC<TransactionModalProps> = ({
  account,
  type,
  onClose,
  onSubmit,
  isLoading,
}) => {
  const [amount, setAmount] = useState<number | ''>('');
  const [description, setDescription] = useState<string>('');
  const [error, setError] = useState<string | null>(null);

  const isDeposit = type === 'DEPOSIT';

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!amount || Number(amount) <= 0) {
      setError('El monto debe ser mayor a cero.');
      return;
    }

    if (!isDeposit && Number(amount) > account.availableBalance) {
      setError(`Fondos insuficientes. Disponible: $${account.availableBalance.toLocaleString('es-CO')}`);
      return;
    }

    try {
      await onSubmit(account.accountNumber, Number(amount), description || undefined);
      onClose();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error en la transacción');
      }
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && (
        <div className="p-3 bg-red-50 text-red-700 text-xs rounded-lg border border-red-200">
          {error}
        </div>
      )}

      <div className="bg-slate-50 p-3 rounded-lg border border-slate-100 text-xs space-y-1">
        <div className="flex justify-between">
          <span className="text-slate-500">Cuenta:</span>
          <span className="font-mono font-bold text-slate-800">{account.accountNumber}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-slate-500">Saldo Disponible:</span>
          <span className="font-bold text-emerald-700">
            ${account.availableBalance.toLocaleString('es-CO', { minimumFractionDigits: 2 })}
          </span>
        </div>
      </div>

      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1">
          Monto a {isDeposit ? 'Consignar' : 'Retirar'} (COP)
        </label>
        <input
          type="number"
          step="0.01"
          min="0.01"
          required
          autoFocus
          value={amount}
          onChange={(e) => setAmount(e.target.value === '' ? '' : parseFloat(e.target.value))}
          placeholder="0.00"
          className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
        />
      </div>

      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1">Descripción (Opcional)</label>
        <input
          type="text"
          maxLength={255}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder={`Ej: ${isDeposit ? 'Consignación nómina' : 'Retiro cajero'}`}
          className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
        />
      </div>

      <div className="flex justify-end gap-3 pt-3 border-t">
        <button
          type="button"
          onClick={onClose}
          disabled={isLoading}
          className="px-4 py-2 border rounded-lg text-sm text-slate-600 hover:bg-slate-50 font-medium"
        >
          Cancelar
        </button>
        <button
          type="submit"
          disabled={isLoading}
          className={`px-4 py-2 text-white rounded-lg text-sm font-medium shadow-sm transition-all disabled:opacity-50 ${
            isDeposit ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-rose-600 hover:bg-rose-700'
          }`}
        >
          {isLoading ? 'Procesando...' : isDeposit ? 'Realizar Consignación' : 'Realizar Retiro'}
        </button>
      </div>
    </form>
  );
};