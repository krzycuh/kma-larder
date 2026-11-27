import { useState } from 'react';
import type { FormEvent } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { pantriesApi } from '../api/pantries';
import { useToast } from '../hooks/useToast';
import { t } from '../i18n';
import type { Pantry } from '../types/pantry';
import { getApiErrorMessage } from '../utils/apiError';

interface CreatePantryFormProps {
  /** Distinguishes the field when the form appears more than once on a page */
  id?: string;
  onCreated?: (pantry: Pantry) => void;
}

/**
 * Name + button: creates a pantry owned by the current user.
 */
export function CreatePantryForm({ id = 'new-pantry-name', onCreated }: CreatePantryFormProps) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [name, setName] = useState('');

  const mutation = useMutation({
    mutationFn: () => pantriesApi.create(name.trim()),
    onSuccess: async (pantry) => {
      await queryClient.invalidateQueries({ queryKey: ['pantries'] });
      setName('');
      toast.success(t.pantries.created(pantry.name));
      onCreated?.(pantry);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const onSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!name.trim()) return toast.error(t.members.nameRequired);
    mutation.mutate();
  };

  return (
    <form onSubmit={onSubmit} noValidate>
      <label htmlFor={id} className="block text-left text-sm font-medium text-gray-700">
        {t.pantries.nameLabel}
      </label>
      <div className="mt-1 flex gap-2">
        <input
          id={id}
          type="text"
          maxLength={255}
          value={name}
          placeholder={t.pantries.namePlaceholder}
          onChange={(e) => setName(e.target.value)}
          className="w-full min-w-0 rounded-md border border-gray-300 px-3 py-2 text-base text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
        <button
          type="submit"
          disabled={mutation.isPending || !name.trim()}
          className="flex-shrink-0 rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50"
        >
          {mutation.isPending ? t.pantries.creating : t.pantries.create}
        </button>
      </div>
    </form>
  );
}
