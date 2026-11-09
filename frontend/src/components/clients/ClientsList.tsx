import React, { useState } from 'react';
import { Client } from '../../types';
import { UserPlus, Edit2, Trash2, CreditCard, Calendar, Mail, FileText } from 'lucide-react';

interface ClientsListProps {
  clients: Client[];
  onAddClient: () => void;
  onEditClient: (client: Client) => void;
  onDeleteClient: (client: Client) => void;
  onSelectClient: (client: Client) => void;
  selectedClient: Client | null;
}

export const ClientsList: React.FC<ClientsListProps> = ({
  clients,
  onAddClient,
  onEditClient,
  onDeleteClient,
  onSelectClient,
  selectedClient,
}) => {
  const [searchTerm, setSearchTerm] = useState('');

  const filteredClients = clients.filter(
    (c) =>
      c.firstName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.lastName.toLowerCase().includes(searchTerm.toLowerCase()) ||
      c.identificationNumber.includes(searchTerm) ||
      c.email.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="bg-white rounded-xl shadow-sm border border-slate-200 overflow-hidden">
      <div className="p-5 border-b border-slate-200 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-bold text-slate-800">Clientes Registrados</h2>
          <p className="text-xs text-slate-500">Administración de clientes y sus productos financieros</p>
        </div>
        <div className="flex items-center gap-3">
          <input
            type="text"
            placeholder="Buscar por cédula o nombre..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="px-3 py-1.5 border rounded-lg text-sm w-48 sm:w-64 focus:ring-2 focus:ring-blue-500 focus:outline-none"
          />
          <button
            onClick={onAddClient}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-blue-600 text-white rounded-lg text-sm font-semibold hover:bg-blue-700 transition shadow-sm"
          >
            <UserPlus className="w-4 h-4" />
            <span>Nuevo</span>
          </button>
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left border-collapse text-sm">
          <thead>
            <tr className="bg-slate-50 text-slate-600 border-b border-slate-200 text-xs uppercase font-semibold">
              <th className="py-3 px-4">Identificación</th>
              <th className="py-3 px-4">Cliente</th>
              <th className="py-3 px-4">Contacto</th>
              <th className="py-3 px-4">Nacimiento</th>
              <th className="py-3 px-4">Fechas Reg.</th>
              <th className="py-3 px-4 text-center">Acciones</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {filteredClients.length === 0 ? (
              <tr>
                <td colSpan={6} className="py-8 text-center text-slate-400">
                  No se encontraron clientes registrados.
                </td>
              </tr>
            ) : (
              filteredClients.map((c) => {
                const isSelected = selectedClient?.id === c.id;
                return (
                  <tr
                    key={c.id}
                    onClick={() => onSelectClient(c)}
                    className={`cursor-pointer transition-colors ${
                      isSelected ? 'bg-blue-50/70 border-l-4 border-l-blue-600' : 'hover:bg-slate-50'
                    }`}
                  >
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-2">
                        <FileText className="w-4 h-4 text-slate-400" />
                        <div>
                          <span className="font-semibold text-slate-800">{c.identificationNumber}</span>
                          <span className="ml-1 text-xs px-1.5 py-0.5 bg-slate-100 text-slate-600 rounded">
                            {c.identificationType}
                          </span>
                        </div>
                      </div>
                    </td>
                    <td className="py-3 px-4">
                      <div className="font-medium text-slate-800">
                        {c.firstName} {c.lastName}
                      </div>
                      <div className="text-xs text-slate-400">ID: #{c.id}</div>
                    </td>
                    <td className="py-3 px-4">
                      <div className="flex items-center gap-1.5 text-slate-600">
                        <Mail className="w-3.5 h-3.5 text-slate-400" />
                        <span>{c.email}</span>
                      </div>
                    </td>
                    <td className="py-3 px-4 text-slate-600">
                      <div className="flex items-center gap-1.5">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        <span>{c.birthDate}</span>
                      </div>
                    </td>
                    <td className="py-3 px-4 text-xs text-slate-500">
                      <div>Creación: {c.createdAt.slice(0, 10)}</div>
                      {c.updatedAt && <div className="text-emerald-600">Modif: {c.updatedAt.slice(0, 10)}</div>}
                    </td>
                    <td className="py-3 px-4 text-center">
                      <div className="flex items-center justify-center gap-2" onClick={(e) => e.stopPropagation()}>
                        <button
                          title="Gestionar Cuentas"
                          onClick={() => onSelectClient(c)}
                          className={`p-1.5 rounded-lg border transition ${
                            isSelected
                              ? 'bg-blue-600 text-white border-blue-600'
                              : 'text-blue-600 hover:bg-blue-50 border-slate-200'
                          }`}
                        >
                          <CreditCard className="w-4 h-4" />
                        </button>
                        <button
                          title="Modificar Cliente"
                          onClick={() => onEditClient(c)}
                          className="p-1.5 rounded-lg border border-slate-200 text-amber-600 hover:bg-amber-50 transition"
                        >
                          <Edit2 className="w-4 h-4" />
                        </button>
                        <button
                          title="Eliminar Cliente"
                          onClick={() => onDeleteClient(c)}
                          className="p-1.5 rounded-lg border border-slate-200 text-red-600 hover:bg-red-50 transition"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};