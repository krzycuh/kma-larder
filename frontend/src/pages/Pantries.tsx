import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { pantriesApi } from '../api/pantries';
import { PageHeader } from '../components/PageHeader';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { ConfirmationModal } from '../components/ConfirmationModal';
import { CreatePantryForm } from '../components/CreatePantryForm';
import { NoPantry } from '../components/NoPantry';
import { useAuth } from '../hooks/useAuth';
import { setCurrentPantryId, usePantries } from '../hooks/usePantries';
import { useToast } from '../hooks/useToast';
import { useViewAs } from '../hooks/useViewAs';
import { t } from '../i18n';
import type { Pantry } from '../types/pantry';
import { getApiErrorMessage } from '../utils/apiError';

const secondaryButton =
  'inline-flex items-center rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50';
const dangerButton =
  'inline-flex items-center rounded-md border border-red-300 bg-white px-3 py-1.5 text-sm font-medium text-red-700 hover:bg-red-50 focus:outline-none focus:ring-2 focus:ring-red-500';

/**
 * /pantries: every pantry the user owns or belongs to. Pick the default one, open its members,
 * create another, delete an owned one or leave somebody else's.
 */
export function Pantries() {
  const queryClient = useQueryClient();
  const toast = useToast();
  const { user } = useAuth();
  const { isViewing } = useViewAs();
  const { data: pantries = [], isLoading, isError, error, refetch } = usePantries();
  const [removing, setRemoving] = useState<Pantry | null>(null);

  const refresh = () => queryClient.invalidateQueries({ queryKey: ['pantries'] });

  const defaultMutation = useMutation({
    mutationFn: (pantry: Pantry) => pantriesApi.setDefault(pantry.id),
    onSuccess: async (_, pantry) => {
      await refresh();
      // The new default is also where the pages go now, whatever was switched to before
      setCurrentPantryId(null);
      toast.success(t.pantries.defaultSet(pantry.name));
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  // Owner: the pantry goes away for everyone. Member: only their own membership does.
  const removeMutation = useMutation({
    mutationFn: (pantry: Pantry) =>
      pantry.myRole === 'OWNER' ? pantriesApi.remove(pantry.id) : pantriesApi.removeMember(pantry.id, user!.id),
    onSuccess: async (_, pantry) => {
      setRemoving(null);
      await refresh();
      queryClient.invalidateQueries({ queryKey: ['dashboard'] });
      toast.success(pantry.myRole === 'OWNER' ? t.pantries.deleted : t.members.left);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  if (isLoading) return <LoadingSpinner text={t.common.loading} />;
  if (isError) return <ErrorMessage message={getApiErrorMessage(error)} retry={() => refetch()} />;

  const header = <PageHeader title={t.pantries.title} description={t.pantries.subtitle} backTo="/" backLabel={t.nav.home} />;

  if (pantries.length === 0) {
    return (
      <div className="mx-auto max-w-2xl">
        {header}
        <NoPantry />
      </div>
    );
  }

  const deleting = removing?.myRole === 'OWNER';

  return (
    <div className="mx-auto max-w-2xl">
      {header}

      <ul className="space-y-3">
        {pantries.map((pantry) => {
          const isOwner = pantry.myRole === 'OWNER';
          return (
            <li key={pantry.id} className="rounded-lg bg-white p-4 shadow-sm sm:p-5">
              <div className="flex flex-wrap items-center gap-x-2 gap-y-1">
                <h2 className="min-w-0 break-words text-lg font-semibold text-gray-900">{pantry.name}</h2>
                {pantry.isDefault && (
                  <span className="rounded-full bg-green-100 px-2.5 py-0.5 text-xs font-medium text-green-800">
                    {t.pantries.defaultBadge}
                  </span>
                )}
              </div>
              <p className="mt-1 text-sm text-gray-600">
                {isOwner ? t.members.roleOwner : t.members.roleMember} · {t.pantries.people(pantry.memberCount)}
              </p>
              <div className="mt-3 flex flex-wrap gap-2">
                {!pantry.isDefault && !isViewing && (
                  <button
                    type="button"
                    onClick={() => defaultMutation.mutate(pantry)}
                    disabled={defaultMutation.isPending}
                    aria-label={`${t.pantries.setDefault}: ${pantry.name}`}
                    className={secondaryButton}
                  >
                    {t.pantries.setDefault}
                  </button>
                )}
                <Link
                  to={`/pantries/${pantry.id}/members`}
                  aria-label={`${t.pantries.members}: ${pantry.name}`}
                  className={secondaryButton}
                >
                  {t.pantries.members}
                </Link>
                {!isViewing && (
                  <button
                    type="button"
                    onClick={() => setRemoving(pantry)}
                    aria-label={`${isOwner ? t.pantries.deleteFull : t.members.leaveFull}: ${pantry.name}`}
                    className={dangerButton}
                  >
                    {isOwner ? t.pantries.delete : t.members.leave}
                  </button>
                )}
              </div>
            </li>
          );
        })}
      </ul>
      {pantries.length > 1 && <p className="mt-3 text-sm text-gray-600">{t.pantries.defaultHint}</p>}

      {!isViewing && (
        <section className="mt-6 rounded-lg bg-white p-4 shadow-sm sm:p-6" aria-labelledby="new-pantry-heading">
          <h2 id="new-pantry-heading" className="text-lg font-semibold text-gray-900">
            {t.pantries.newSection}
          </h2>
          <p className="mb-3 mt-1 text-sm text-gray-600">{t.pantries.newHint}</p>
          <CreatePantryForm />
        </section>
      )}

      <ConfirmationModal
        isOpen={removing !== null}
        onClose={() => setRemoving(null)}
        onConfirm={() => removing && removeMutation.mutate(removing)}
        title={deleting ? t.pantries.deleteConfirmTitle(removing?.name ?? '') : t.members.leaveConfirmTitle}
        description={
          deleting ? t.pantries.deleteConfirmText(removing?.memberCount ?? 1) : t.members.leaveConfirmText(removing?.name ?? '')
        }
        confirmText={deleting ? t.pantries.deleteFull : t.members.leaveFull}
        cancelText={t.common.cancel}
        variant="danger"
        isLoading={removeMutation.isPending}
      />
    </div>
  );
}
