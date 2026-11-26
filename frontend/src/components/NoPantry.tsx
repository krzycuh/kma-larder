import { useAuth } from '../hooks/useAuth';
import { useViewAs } from '../hooks/useViewAs';
import { t } from '../i18n';
import { CreatePantryForm } from './CreatePantryForm';
import { EmptyState } from './EmptyState';

/**
 * Shown instead of a stock page while the user has no pantry: create the household's one,
 * or ask its owner to add you (they need the e-mail of this account).
 */
export function NoPantry() {
  const { user } = useAuth();
  const { isViewing } = useViewAs();

  if (isViewing) return <EmptyState icon="🫙" title={t.pantry.noPantryTitle} description={t.pantry.noPantryViewed} />;

  return (
    <div className="mx-auto max-w-xl rounded-lg bg-white shadow-sm">
      <EmptyState icon="🫙" title={t.pantry.noPantryTitle} description={t.pantry.noPantry}>
        <div className="mx-auto max-w-md">
          <CreatePantryForm id="first-pantry-name" />
          <p className="mt-6 text-sm text-gray-600">{t.pantry.noPantryJoin(user?.email ?? '')}</p>
        </div>
      </EmptyState>
    </div>
  );
}
