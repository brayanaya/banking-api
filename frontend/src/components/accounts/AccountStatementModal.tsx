import React, { useState, useEffect } from 'react';
import { Account, AccountStatement } from '../../types';
import { api } from '../../services/api';
import { ArrowDownLeft, ArrowUpRight, ArrowRightLeft } from 'lucide-react';

interface AccountStatementModalProps {
  account: Account;
  onClose: () => void;
}

export const AccountStatementModal: React.FC<AccountStatementModalProps> = ({ account, onClose }) => {
  const [statement, setStatement] = useState<AccountStatement | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  const [from, setFrom] = useState<string>('');
  const [to, setTo] = useState<string>('');

  const fetchStatement = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await api.getStatement(account.id, from || undefined, to || undefined);
      setStatement(data);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setError(err.message);
      } else {
        setError('Error al cargar extracto');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchStatement();
  }, []);

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('es-CO', {
      style: 'currency',
      currency: 'COP',
      minimumFractionDigits: 2,
    }).format(val);
  };

  return (
    <div className="space-y-4">
      {error && (
        <div className="p-3 bg-red-50 text-red-700 text-xs rounded-lg border border-red-200">
          {error}
        </div>
      )}

      {/* Date filter */}
      <div className="flex flex-wrap items-center gap-2 bg-slate-50 p-2.5 rounded-lg border border-slate-200 text-xs">
        <div className="flex items-center gap-1">
          <span className="text-slate-500 font-medium">Desde:</span>
          <input
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            className="px-2 py-1 border rounded bg-white"
          />
        </div>
        <div className="flex items-center gap-1">
          <span className="text-slate-500 font-medium">Hasta:</span>
          <input
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            className="px-2 py-1 border rounded bg-white"
          />
        </div>
        <button
          onClick={fetchStatement}
          className="ml-auto px-3 py-1 bg-blue-600 text-white rounded font-medium hover:bg-blue-700 transition"
        >
          Filtrar
        </button>
      </div>

      {loading ? (
        <div className="text-center py-10 text-slate-400 text-sm">Cargando estado de cuenta...</div>
      ) : statement ? (
        <>
          {/* Summary Cards */}
          <div className="grid grid-cols-3 gap-3">
            <div className="p-3 bg-slate-50 rounded-lg border border-slate-200 text-center">
              <span className="block text-[11px] text-slate-500 font-medium uppercase">Saldo Final</span>
              <span className={`text-base font-bold ${statement.account.balance < 0 ? 'text-red-600' : 'text-slate-800'}`}>
                {formatCurrency(statement.account.balance)}
              </span>
            </div>
            <div className="p-3 bg-emerald-50 rounded-lg border border-emerald-200 text-center">
              <span className="block text-[11px] text-emerald-700 font-medium uppercase">Total Créditos (+)</span>
              <span className="text-base font-bold text-emerald-800">
                {formatCurrency(statement.totalCredits)}
              </span>
            </div>
            <div className="p-3 bg-rose-50 rounded-lg border border-rose-200 text-center">
              <span className="block text-[11px] text-rose-700 font-medium uppercase">Total Débitos (-)</span>
              <span className="text-base font-bold text-rose-800">
                {formatCurrency(statement.totalDebits)}
              </span>
            </div>
          </div>

          {/* Movements Table */}
          <div className="max-h-64 overflow-y-auto border rounded-lg">
            <table className="w-full text-left text-xs border-collapse">
              <thead className="bg-slate-50 sticky top-0 border-b text-slate-600">
                <tr>
                  <th className="p-2.5">Fecha</th>
                  <th className="p-2.5">Tipo / Concepto</th>
                  <th className="p-2.5 text-right">Monto</th>
                  <th className="p-2.5 text-right">Saldo Posterior</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {statement.movements.length === 0 ? (
                  <tr>
                    <td colSpan={4} className="p-6 text-center text-slate-400">
                      No se registraron movimientos en este período.
                    </td>
                  </tr>
                ) : (
                  statement.movements.map((m) => {
                    const isCredit = m.movementType === 'CREDIT';
                    return (
                      <tr key={m.id} className="hover:bg-slate-50">
                        <td className="p-2.5 font-mono text-slate-500">
                          {m.createdAt.slice(0, 16).replace('T', ' ')}
                        </td>
                        <td className="p-2.5">
                          <div className="flex items-center gap-1.5 font-medium">
                            {m.transactionType === 'DEPOSIT' && <ArrowDownLeft className="w-3.5 h-3.5 text-emerald-600" />}
                            {m.transactionType === 'WITHDRAWAL' && <ArrowUpRight className="w-3.5 h-3.5 text-rose-600" />}
                            {m.transactionType === 'TRANSFER' && <ArrowRightLeft className="w-3.5 h-3.5 text-blue-600" />}
                            <span className="text-slate-800">{m.transactionType}</span>
                          </div>
                          {m.description && <div className="text-[11px] text-slate-400">{m.description}</div>}
                          {m.relatedAccountNumber && (
                            <div className="text-[10px] text-blue-500">Cuenta Relacionada: {m.relatedAccountNumber}</div>
                          )}
                        </td>
                        <td className={`p-2.5 text-right font-bold ${isCredit ? 'text-emerald-600' : 'text-rose-600'}`}>
                          {isCredit ? '+' : '-'}{formatCurrency(m.amount)}
                        </td>
                        <td className="p-2.5 text-right font-mono text-slate-700">
                          {formatCurrency(m.balanceAfter)}
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </>
      ) : null}

      <div className="flex justify-end pt-3 border-t">
        <button
          onClick={onClose}
          className="px-4 py-2 bg-slate-800 text-white rounded-lg text-sm font-medium hover:bg-slate-900 transition"
        >
          Cerrar
        </button>
      </div>
    </div>
  );
};