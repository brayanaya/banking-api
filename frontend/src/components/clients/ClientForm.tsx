import React, { useState } from 'react';
import { Client, ClientPayload, IdentificationType } from '../../types';

interface ClientFormProps {
  initialData?: Client | null;
  onSubmit: (payload: ClientPayload) => Promise<void>;
  onCancel: () => void;
  isLoading: boolean;
}

export const ClientForm: React.FC<ClientFormProps> = ({
  initialData,
  onSubmit,
  onCancel,
  isLoading,
}) => {
  const [formData, setFormData] = useState<ClientPayload>({
    identificationType: initialData?.identificationType || 'CC',
    identificationNumber: initialData?.identificationNumber || '',
    firstName: initialData?.firstName || '',
    lastName: initialData?.lastName || '',
    email: initialData?.email || '',
    birthDate: initialData?.birthDate || '',
  });

  const [formError, setFormError] = useState<string | null>(null);

  const calculateAge = (birthDateStr: string) => {
    const today = new Date();
    const birthDate = new Date(birthDateStr);
    let age = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
      age--;
    }
    return age;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);

    // Front validations aligning with PDF requirements
    if (formData.firstName.trim().length < 2) {
      setFormError('Los nombres deben tener al menos 2 caracteres.');
      return;
    }
    if (formData.lastName.trim().length < 2) {
      setFormError('El apellido debe tener al menos 2 caracteres.');
      return;
    }
    const emailPattern = /^[^@\s]+@[^@\s]+\.[^@\s]{2,}$/;
    if (!emailPattern.test(formData.email)) {
      setFormError('El formato del correo debe ser válido (ej. xxxx@xxxxx.xxx).');
      return;
    }
    if (!formData.birthDate) {
      setFormError('La fecha de nacimiento es obligatoria.');
      return;
    }
    const age = calculateAge(formData.birthDate);
    if (age < 18) {
      setFormError(`El cliente debe ser mayor de edad (tiene ${age} años).`);
      return;
    }

    try {
      await onSubmit(formData);
    } catch (err: unknown) {
      if (err instanceof Error) {
        setFormError(err.message);
      } else {
        setFormError('Ocurrió un error inesperado');
      }
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {formError && (
        <div className="p-3 bg-red-50 text-red-700 text-sm rounded-lg border border-red-200">
          {formError}
        </div>
      )}

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-semibold text-slate-600 mb-1">
            Tipo de Identificación
          </label>
          <select
            value={formData.identificationType}
            onChange={(e) => setFormData({ ...formData, identificationType: e.target.value as IdentificationType })}
            disabled={!!initialData}
            className="w-full px-3 py-2 border rounded-lg text-sm bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none disabled:bg-slate-100"
          >
            <option value="CC">Cédula de Ciudadanía (CC)</option>
            <option value="CE">Cédula de Extranjería (CE)</option>
            <option value="PASSPORT">Pasaporte (PASSPORT)</option>
            <option value="NIT">NIT</option>
          </select>
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-600 mb-1">
            Número de Identificación
          </label>
          <input
            type="text"
            required
            value={formData.identificationNumber}
            onChange={(e) => setFormData({ ...formData, identificationNumber: e.target.value })}
            disabled={!!initialData}
            placeholder="Ej: 1075304928"
            className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none disabled:bg-slate-100"
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-xs font-semibold text-slate-600 mb-1">Nombres</label>
          <input
            type="text"
            required
            minLength={2}
            value={formData.firstName}
            onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
            placeholder="Ej: Carlos Alberto"
            className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
          />
        </div>

        <div>
          <label className="block text-xs font-semibold text-slate-600 mb-1">Apellidos</label>
          <input
            type="text"
            required
            minLength={2}
            value={formData.lastName}
            onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
            placeholder="Ej: Gomez Perez"
            className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
          />
        </div>
      </div>

      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1">Correo Electrónico</label>
        <input
          type="email"
          required
          value={formData.email}
          onChange={(e) => setFormData({ ...formData, email: e.target.value })}
          placeholder="ejemplo@banco.com"
          className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
        />
      </div>

      <div>
        <label className="block text-xs font-semibold text-slate-600 mb-1">Fecha de Nacimiento</label>
        <input
          type="date"
          required
          value={formData.birthDate}
          onChange={(e) => setFormData({ ...formData, birthDate: e.target.value })}
          className="w-full px-3 py-2 border rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
        />
        <p className="text-xs text-slate-400 mt-1">Debe ser mayor de edad (18+ años).</p>
      </div>

      <div className="flex justify-end gap-3 pt-3 border-t">
        <button
          type="button"
          onClick={onCancel}
          disabled={isLoading}
          className="px-4 py-2 border rounded-lg text-sm text-slate-600 hover:bg-slate-50 font-medium"
        >
          Cancelar
        </button>
        <button
          type="submit"
          disabled={isLoading}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg text-sm font-medium hover:bg-blue-700 shadow-sm transition-all disabled:opacity-50"
        >
          {isLoading ? 'Guardando...' : initialData ? 'Actualizar Cliente' : 'Crear Cliente'}
        </button>
      </div>
    </form>
  );
};