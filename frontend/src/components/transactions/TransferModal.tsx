import React, { useState } from 'react';
import { Account } from '../../types';

interface TransferModalProps {
  originAccount: Account;
  allAccounts: Account[];
  onClose: () => void;
  onSubmit: (originNumber: string, destinationNumber: string, amount: number, description?: string) => Promise<void>;
  isLoading: boolean;
}

export const TransferModal: React.FC<TransferModalProps> = ({
  originAccount,
  allAccounts,
  onClose,
  onSubmit,
  isLoading,
}) => {
  const [destinationNumber, setDestinationNumber] = useState<string>('');
  const [amount, setAmount] = useState<number | ''>('');
  const [description, setDescription] = useState<string>('');
  const [error, setError] = useState<string | null>(null);

  // Available destination accounts (exclude origin)
  const otherAccounts = allAccounts.filter((a) => a.accountNumber !== originAccount.accountNumber && a.status === 'ACTIVE');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!destinationNumber.trim()) {
      setError('Debes ingresar o seleccionar una cuenta destino.');
      return;
    }
    if (destinationNumber.trim() === originAccount.accountNumber) {
      setError('La cuenta origen y destino no pueden ser la misma.');
      return;
    }
    if (!amount || Number(amount) <= 0) {
      setError('El monto a transferir debe ser mayor a cero.');
      return;
    }
    if (Number(amount) > originAccount.availableBalance) {
      setError(`Fondos insuficientes en la cuenta origen. Disponible: $${originAccount.availableBalance.toLocaleString('es-CO')}`);
      return;
    }

    try {
      await onSubmit(originAccount.accountNumber, destinationNumber.trim(), Number(amount), description || undefined);
      onClose();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error en la transferencia');
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
          <span className="text-slate-500">Cuenta Origen:</span>
          <span className="font-mono font-bold text-slate-800">{originAccount.accountNumber}</span>
        </div>
        <div className="flex justify-between">
          <span className="text-slate-500">Saldo Disponible:</span>
          <span className="font-bold text-emerald-700">
            ${originAccount.availableBalance.toLocaleString('es-CO', { minimumFractionDigits: 2 })}
          </span>
        </div>
      </div>

      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1">Cuenta Destino (10 dígitos)</label>
        {otherAccounts.length > 0 && (
          <select
            onChange={(e) => setDestinationNumber(e.target.value)}
            value={destinationNumber}
            className="w-full mb-2 px-3 py-1.5 border rounded-lg text-xs bg-white text-slate-700 focus:outline-none"
          >
            <option value="">-- Seleccionar de cuentas registradas --</option>
            {otherAccounts.map((a) => (
              <option key={a.id} value={a.accountNumber}>
                {a.accountNumber} ({a.type === 'SAVINGS' ? 'Ahorros' : 'Corriente'})
              </option>
            ))}
          </select>
        )}
        <input
          type="text"
          required
          maxLength={10}
          pattern="^\d{10}$"
          value={destinationNumber}
          onChange={(e) => setDestinationNumber(e.target.value)}
          placeholder="O ingresa un número de cuenta (ej. 53... / 33...)"
          className="w-full px-3 py-2 border rounded-lg text-sm font-mono focus:ring-2 focus:ring-blue-500 focus:outline-none"
        />
      </div>

      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1">Monto a Transferir (COP)</label>
        <input
          type="number"
          step="0.01"
          min="0.01"
          required
          value={amount}
          onChange={(e) => setAmount(e.target.value === '' ? '' : parseFloat(e.target.value))}
          placeholder="0.00"
          className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
        />
      </div>

      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1">Concepto o Descripción</label>
        <input
          type="text"
          maxLength={255}
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          placeholder="Ej: Pago de arriendo / transferencia inmediata"
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
          className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg text-sm font-medium shadow-sm transition-all disabled:opacity-50"
        >
          {isLoading ? 'Transfiriendo...' : 'Confirmar Transferencia'}
        </button>
      </div>
    </form>
  );
};