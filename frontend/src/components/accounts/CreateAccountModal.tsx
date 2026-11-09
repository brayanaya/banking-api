import React, { useState } from 'react';
import { AccountType, CreateAccountPayload } from '../../types';

interface CreateAccountModalProps {
  clientId: number;
  clientName: string;
  onSubmit: (payload: CreateAccountPayload) => Promise<void>;
  onClose: () => void;
  isLoading: boolean;
}

export const CreateAccountModal: React.FC<CreateAccountModalProps> = ({
  clientId,
  clientName,
  onSubmit,
  onClose,
  isLoading,
}) => {
  const [type, setType] = useState<AccountType>('SAVINGS');
  const [initialBalance, setInitialBalance] = useState<number>(0);
  const [gmfExempt, setGmfExempt] = useState<boolean>(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (type === 'SAVINGS' && initialBalance < 0) {
      setError('La cuenta de ahorros no puede tener un saldo menor a $0.');
      return;
    }

    try {
      await onSubmit({
        clientId,
        type,
        initialBalance: Number(initialBalance),
        gmfExempt,
      });
      onClose();
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error al crear el producto');
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

      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1">Cliente Beneficiario</label>
        <input
          type="text"
          disabled
          value={clientName}
          className="w-full px-3 py-2 border rounded-lg text-sm bg-slate-100 text-slate-700"
        />
      </div>

      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1">Tipo de Producto</label>
        <select
          value={type}
          onChange={(e) => setType(e.target.value as AccountType)}
          className="w-full px-3 py-2 border rounded-lg text-sm bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
        >
          <option value="SAVINGS">Cuenta de Ahorros (Prefijo 53...)</option>
          <option value="CHECKING">Cuenta Corriente (Prefijo 33... Cupo $3M)</option>
        </select>
        <p className="text-[11px] text-slate-400 mt-1">
          {type === 'SAVINGS'
            ? 'El número iniciará en 53 y se inicializa activa.'
            : 'El número iniciará en 33 con sobregiro automático hasta $3,000,000.'}
        </p>
      </div>

      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1">Saldo Inicial (COP)</label>
        <input
          type="number"
          step="0.01"
          min="0"
          required
          value={initialBalance}
          onChange={(e) => setInitialBalance(parseFloat(e.target.value) || 0)}
          className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
        />
      </div>

      <div className="flex items-center gap-2 pt-2">
        <input
          type="checkbox"
          id="gmf"
          checked={gmfExempt}
          onChange={(e) => setGmfExempt(e.target.checked)}
          className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
        />
        <label htmlFor="gmf" className="text-xs text-slate-700 select-none">
          Marcar cuenta como exenta de 4x1000 (GMF)
        </label>
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
          className="px-4 py-2 bg-emerald-600 text-white rounded-lg text-sm font-medium hover:bg-emerald-700 shadow-sm transition-all disabled:opacity-50"
        >
          {isLoading ? 'Creando...' : 'Crear Cuenta'}
        </button>
      </div>
    </form>
  );
};