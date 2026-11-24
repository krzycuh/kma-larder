import { useAuth } from '../hooks/useAuth';
import { t } from '../i18n';

/**
 * Home dashboard (placeholder until the stock pages land).
 */
export function Dashboard() {
  const { user } = useAuth();
  const firstName = user?.name?.trim().split(/\s+/)[0] || '';

  return (
    <div className="mx-auto max-w-5xl">
      <h1 className="text-2xl font-bold text-gray-900 sm:text-3xl">{t.dashboard.greeting(firstName)}</h1>
      <p className="mt-1 text-sm text-gray-600">{t.app.tagline}</p>
    </div>
  );
}
