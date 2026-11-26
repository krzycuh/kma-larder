import { useQuery } from '@tanstack/react-query';
import { pantryStockApi } from '../api/stock';
import { PageHeader } from '../components/PageHeader';
import { PantrySwitcher } from '../components/PantrySwitcher';
import { EmptyState } from '../components/EmptyState';
import { NoPantry } from '../components/NoPantry';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { BatchCard } from '../components/BatchCard';
import { useCurrentPantry } from '../hooks/usePantries';
import { useListSearchParams } from '../hooks/useListSearchParams';
import { t } from '../i18n';
import type { ExpiringParams } from '../types/stock';

type Tab = 'all' | 'expired' | '30' | '90' | 'nodate';
const TABS: { key: Tab; label: string; params: ExpiringParams }[] = [
  { key: 'all', label: t.expiring.tabAll, params: {} },
  { key: 'expired', label: t.expiring.tabExpired, params: { expiredOnly: true } },
  { key: '30', label: t.expiring.tab30, params: { withinDays: 30 } },
  { key: '90', label: t.expiring.tab90, params: { withinDays: 90 } },
  { key: 'nodate', label: t.expiring.tabNoDate, params: { undatedOnly: true } },
];

/**
 * Best-before dates across the pantry: what to use up first, with the consume buttons right there.
 */
export function Expiring() {
  const { pantry, isLoading: pantryLoading, isError: pantryError } = useCurrentPantry();
  const { get, setFilters } = useListSearchParams();
  const tab = TABS.find((item) => item.key === get('tab')) ?? TABS[0];

  const query = useQuery({
    queryKey: ['expiring', pantry?.id, tab.key],
    queryFn: () => pantryStockApi.expiring(pantry!.id, tab.params),
    enabled: !!pantry,
  });

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader title={t.expiring.title} description={t.expiring.subtitle} backLabel={t.nav.home} />
      <div className="mb-4">
        <PantrySwitcher />
      </div>
      <div className="mb-4 flex gap-1 overflow-x-auto rounded-lg bg-gray-100 p-1" role="tablist">
        {TABS.map((item) => (
          <button
            key={item.key}
            type="button"
            role="tab"
            aria-selected={tab.key === item.key}
            onClick={() => setFilters({ tab: item.key === 'all' ? undefined : item.key })}
            className={`whitespace-nowrap rounded-md px-3 py-1.5 text-sm font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 ${
              tab.key === item.key ? 'bg-white text-blue-700 shadow-sm' : 'text-gray-600 hover:text-gray-900'
            }`}
          >
            {item.label}
          </button>
        ))}
      </div>

      {pantryLoading || (pantry && query.isLoading) ? (
        <LoadingSkeleton count={3} />
      ) : pantryError || query.isError ? (
        <ErrorMessage message={t.common.errorGeneric} retry={() => query.refetch()} />
      ) : !pantry ? (
        <NoPantry />
      ) : query.data && query.data.length === 0 ? (
        <EmptyState icon="✅" title={t.expiring.emptyTitle} description={t.expiring.emptyText} />
      ) : (
        <>
          <p className="mb-2 text-sm text-gray-500">{t.expiring.count(query.data?.length ?? 0)}</p>
          <ul className="space-y-2">
            {query.data?.map((batch) => <BatchCard key={batch.id} batch={batch} product={batch.product} showProduct />)}
          </ul>
        </>
      )}
    </div>
  );
}
