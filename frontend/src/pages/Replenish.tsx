import { Link } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { pantryStockApi } from '../api/stock';
import { PageHeader } from '../components/PageHeader';
import { PantrySwitcher } from '../components/PantrySwitcher';
import { EmptyState } from '../components/EmptyState';
import { NoPantry } from '../components/NoPantry';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { useCurrentPantry } from '../hooks/usePantries';
import { t } from '../i18n';
import { ProductThumb } from '../components/ProductThumb';

/**
 * Shopping list: products below their minimum stock, biggest gap first.
 */
export function Replenish() {
  const { pantry, isLoading: pantryLoading, isError: pantryError } = useCurrentPantry();
  const query = useQuery({
    queryKey: ['replenishment', pantry?.id],
    queryFn: () => pantryStockApi.replenishment(pantry!.id),
    enabled: !!pantry,
  });

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader title={t.replenish.title} description={t.replenish.subtitle} backLabel={t.nav.home} />
      <div className="mb-4">
        <PantrySwitcher />
      </div>
      {pantryLoading || (pantry && query.isLoading) ? (
        <LoadingSkeleton count={3} />
      ) : pantryError || query.isError ? (
        <ErrorMessage message={t.common.errorGeneric} retry={() => query.refetch()} />
      ) : !pantry ? (
        <NoPantry />
      ) : query.data && query.data.length === 0 ? (
        <EmptyState icon="🧺" title={t.replenish.emptyTitle} description={t.replenish.emptyText} />
      ) : (
        <>
          <p className="mb-2 text-sm text-gray-500">{t.replenish.count(query.data?.length ?? 0)}</p>
          <ul className="divide-y divide-gray-100 rounded-lg bg-white shadow-sm">
            {query.data?.map((item) => (
              <li key={item.product.id} className="flex items-center gap-3 p-3 sm:p-4">
                <ProductThumb product={item.product} />
                <div className="min-w-0 flex-1">
                  <Link to={`/products/${item.product.id}`} className="block truncate font-semibold text-gray-900 hover:text-blue-700">
                    {item.product.name}
                    {item.product.packageSize && <span className="ml-1 text-sm font-normal text-gray-500">{item.product.packageSize}</span>}
                  </Link>
                  <p className="text-xs text-gray-500">
                    {t.replenish.have(item.quantity)} · {t.replenish.want(item.minQuantity)}
                  </p>
                </div>
                <span className="rounded-full bg-blue-100 px-3 py-1 text-sm font-semibold text-blue-800">{t.replenish.missing(item.missing)}</span>
                <Link
                  to={`/add?productId=${item.product.id}`}
                  className="btn hidden rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 sm:inline-flex"
                >
                  + {t.product.addBatch.replace('+ ', '')}
                </Link>
              </li>
            ))}
          </ul>
        </>
      )}
    </div>
  );
}
