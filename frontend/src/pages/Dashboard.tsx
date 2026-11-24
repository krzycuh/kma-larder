import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { pantryStockApi } from '../api/stock';
import { useAuth } from '../hooks/useAuth';
import { useCurrentPantry } from '../hooks/usePantries';
import { t } from '../i18n';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { ErrorMessage } from '../components/ErrorMessage';
import { EmptyState } from '../components/EmptyState';
import { FreshnessBadge } from '../components/FreshnessBadge';
import { NavIcon } from '../components/NavIcon';
import { PantrySwitcher } from '../components/PantrySwitcher';
import { formatDate } from '../utils/format';
import { getApiErrorMessage } from '../utils/apiError';
import { describeDaysLeft } from '../utils/freshness';
import { CATEGORY_ICONS } from '../utils/categories';

interface StatTileProps {
  label: string;
  value: string | number;
  hint?: string;
  tone?: 'neutral' | 'red' | 'amber';
  to?: string;
}

function StatTile({ label, value, hint, tone = 'neutral', to }: StatTileProps) {
  const valueClass = tone === 'red' ? 'text-red-700' : tone === 'amber' ? 'text-amber-700' : 'text-gray-900';
  const content = (
    <>
      <p className="text-sm font-medium text-gray-600">{label}</p>
      <p className={`mt-1 text-3xl font-bold ${valueClass}`}>{value}</p>
      {hint && <p className="mt-1 text-xs text-gray-500">{hint}</p>}
    </>
  );
  const classes = 'block rounded-lg bg-white p-4 shadow-sm sm:p-5';
  if (to) {
    return (
      <Link to={to} className={`${classes} transition-colors hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500`}>
        {content}
      </Link>
    );
  }
  return <div className={classes}>{content}</div>;
}

const listRowClasses = '-mx-2 flex items-center gap-3 rounded-md px-2 py-3 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500';

function SeeAllLink({ to, label }: { to: string; label: string }) {
  return (
    <Link to={to} aria-label={label} className="shrink-0 whitespace-nowrap text-sm font-medium text-blue-600 hover:text-blue-700">
      <span className="sm:hidden">{t.dashboard.seeAll}</span>
      <span className="hidden sm:inline">{label}</span> →
    </Link>
  );
}

/**
 * Home: what to use up first, what is missing, what changed lately; one tap to add stock.
 */
export function Dashboard() {
  const { user } = useAuth();
  const { pantry, isLoading: pantryLoading, isError: pantryError } = useCurrentPantry();
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['dashboard', pantry?.id],
    queryFn: () => pantryStockApi.dashboard(pantry!.id),
    enabled: !!pantry,
  });

  const firstName = user?.name?.trim().split(/\s+/)[0] || '';

  return (
    <div className="mx-auto max-w-5xl">
      <div className="mb-4 flex flex-col gap-3 sm:mb-6 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <h1 className="text-2xl font-bold text-gray-900 sm:text-3xl">{t.dashboard.greeting(firstName)}</h1>
          <p className="mt-1 text-sm text-gray-600">{t.app.tagline}</p>
        </div>
        <div className="flex flex-col gap-2 sm:flex-row">
          <Link
            to="/add"
            className="rounded-lg bg-blue-600 px-4 py-2 text-center text-sm font-semibold text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            {t.dashboard.add}
          </Link>
          <Link
            to="/add?scan=1"
            className="flex items-center justify-center gap-2 rounded-lg border border-gray-300 bg-white px-4 py-2 text-center text-sm font-semibold text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            <NavIcon name="camera" />
            {t.dashboard.scan}
          </Link>
        </div>
      </div>

      <div className="mb-4">
        <PantrySwitcher />
      </div>

      {(pantryLoading || (pantry && isLoading)) && (
        <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
          <LoadingSkeleton variant="stat" />
          <LoadingSkeleton variant="stat" />
          <LoadingSkeleton variant="stat" />
          <LoadingSkeleton variant="stat" />
        </div>
      )}

      {(pantryError || isError) && <ErrorMessage message={getApiErrorMessage(error)} retry={() => refetch()} />}
      {!pantryLoading && !pantryError && !pantry && (
        <EmptyState icon="🫙" title={t.dashboard.emptyPantryTitle} description={t.pantry.noPantry} />
      )}

      {data && (
        <>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            <StatTile label={t.dashboard.expired} value={data.expiredCount} tone={data.expiredCount > 0 ? 'red' : 'neutral'} to="/expiring?tab=expired" />
            <StatTile
              label={t.dashboard.expiringSoon(data.soonDays)}
              value={data.expiringSoonCount}
              tone={data.expiringSoonCount > 0 ? 'amber' : 'neutral'}
              to="/expiring?tab=30"
            />
            <StatTile label={t.dashboard.belowMinimum} value={data.belowMinimumCount} to="/replenish" />
            <StatTile label={t.dashboard.products} value={data.productCount} hint={t.dashboard.units(data.unitCount)} to="/stock" />
          </div>

          {data.productCount === 0 ? (
            <div className="mt-6 rounded-lg bg-white shadow-sm">
              <EmptyState icon="🫙" title={t.dashboard.emptyPantryTitle} description={t.dashboard.emptyPantryText}>
                <Link to="/add" className="inline-block rounded-lg bg-blue-600 px-6 py-2 text-white hover:bg-blue-700">
                  {t.dashboard.add}
                </Link>
              </EmptyState>
            </div>
          ) : (
            <div className="mt-6 grid grid-cols-1 gap-6 lg:grid-cols-2">
              <section className="min-w-0 rounded-lg bg-white p-4 shadow-sm sm:p-5" aria-labelledby="expiring-heading">
                <div className="mb-3 flex items-baseline justify-between gap-3">
                  <h2 id="expiring-heading" className="text-lg font-semibold text-gray-900">
                    {t.dashboard.expiringSection}
                  </h2>
                  <SeeAllLink to="/expiring" label={t.dashboard.seeAllExpiring} />
                </div>
                {data.expiring.length === 0 ? (
                  <p className="py-6 text-center text-sm text-gray-500">{t.dashboard.emptyExpiring}</p>
                ) : (
                  <ul className="divide-y divide-gray-100">
                    {data.expiring.map((batch) => (
                      <li key={batch.id}>
                        <Link to={`/products/${batch.product.id}`} className={listRowClasses}>
                          <span className="text-2xl" aria-hidden="true">
                            {CATEGORY_ICONS[batch.product.category]}
                          </span>
                          <div className="min-w-0 flex-1">
                            <p className="truncate font-medium text-gray-900">
                              {batch.quantity} × {batch.product.name}
                            </p>
                            <p className="text-xs text-gray-500">{batch.expiresOn ? t.batch.expiresOn(formatDate(batch.expiresOn)) : t.batch.noDate}</p>
                          </div>
                          <FreshnessBadge status={batch.freshness} detail={describeDaysLeft(batch.daysLeft)} />
                        </Link>
                      </li>
                    ))}
                  </ul>
                )}
              </section>

              <section className="min-w-0 rounded-lg bg-white p-4 shadow-sm sm:p-5" aria-labelledby="replenish-heading">
                <div className="mb-3 flex items-baseline justify-between gap-3">
                  <h2 id="replenish-heading" className="text-lg font-semibold text-gray-900">
                    {t.dashboard.replenishSection}
                  </h2>
                  <SeeAllLink to="/replenish" label={t.dashboard.seeAllReplenish} />
                </div>
                {data.belowMinimum.length === 0 ? (
                  <p className="py-6 text-center text-sm text-gray-500">{t.dashboard.emptyReplenish}</p>
                ) : (
                  <ul className="divide-y divide-gray-100">
                    {data.belowMinimum.map((item) => (
                      <li key={item.product.id}>
                        <Link to={`/products/${item.product.id}`} className={listRowClasses}>
                          <span className="text-2xl" aria-hidden="true">
                            {CATEGORY_ICONS[item.product.category]}
                          </span>
                          <div className="min-w-0 flex-1">
                            <p className="truncate font-medium text-gray-900">{item.product.name}</p>
                            <p className="text-xs text-gray-500">
                              {t.replenish.have(item.quantity)} · {t.replenish.want(item.minQuantity)}
                            </p>
                          </div>
                          <span className="rounded-full bg-blue-100 px-2.5 py-0.5 text-xs font-semibold text-blue-800">{t.replenish.missing(item.missing)}</span>
                        </Link>
                      </li>
                    ))}
                  </ul>
                )}
              </section>

              <section className="min-w-0 rounded-lg bg-white p-4 shadow-sm lg:col-span-2 sm:p-5" aria-labelledby="recent-heading">
                <h2 id="recent-heading" className="mb-3 text-lg font-semibold text-gray-900">
                  {t.dashboard.recentSection}
                </h2>
                {data.recentMovements.length === 0 ? (
                  <p className="py-6 text-center text-sm text-gray-500">{t.dashboard.emptyRecent}</p>
                ) : (
                  <ul className="divide-y divide-gray-100 text-sm">
                    {data.recentMovements.map((movement) => (
                      <li key={movement.id} className="flex items-center justify-between gap-3 py-2">
                        <Link to={`/products/${movement.productId}`} className="min-w-0 truncate font-medium text-gray-900 hover:text-blue-700">
                          {movement.productName}
                        </Link>
                        <span className="shrink-0 text-gray-600">
                          {t.movement.type[movement.type]}{' '}
                          <span className={movement.quantity > 0 ? 'font-semibold text-green-700' : 'font-semibold text-gray-900'}>
                            {movement.quantity > 0 ? `+${movement.quantity}` : movement.quantity}
                          </span>
                          <span className="ml-2 text-gray-400">{formatDate(movement.occurredOn)}</span>
                        </span>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            </div>
          )}
        </>
      )}
    </div>
  );
}
