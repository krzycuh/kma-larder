import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { productsApi } from '../api/stock';
import { PageHeader } from '../components/PageHeader';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { ErrorMessage } from '../components/ErrorMessage';
import { EmptyState } from '../components/EmptyState';
import { FreshnessBadge } from '../components/FreshnessBadge';
import { BatchCard } from '../components/BatchCard';
import { ConfirmationModal } from '../components/ConfirmationModal';
import { useInvalidateStock } from '../hooks/useStockMutations';
import { useToast } from '../hooks/useToast';
import { t } from '../i18n';
import { formatDate, currentYear } from '../utils/format';
import { getApiErrorMessage } from '../utils/apiError';
import { CATEGORY_ICONS } from '../utils/categories';
import { listUrl } from '../utils/listUrl';

/**
 * One product: how many units in which batches, what to do with them, and what happened to it over time.
 */
export function ProductDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useToast();
  const invalidate = useInvalidateStock();
  const [removing, setRemoving] = useState(false);

  const query = useQuery({
    queryKey: ['product', id],
    queryFn: () => productsApi.get(id!),
    enabled: !!id,
  });

  const archiveMutation = useMutation({
    mutationFn: (archived: boolean) => {
      const p = query.data!.product;
      return productsApi.update(p.id, {
        name: p.name,
        kind: p.kind,
        category: p.category,
        barcode: p.barcode,
        packageSize: p.packageSize,
        defaultShelfLifeMonths: p.defaultShelfLifeMonths,
        minQuantity: p.minQuantity,
        notes: p.notes,
        archived,
      });
    },
    onSuccess: () => {
      invalidate();
      toast.success(t.product.updated);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const deleteMutation = useMutation({
    mutationFn: () => productsApi.delete(id!),
    onSuccess: () => {
      invalidate();
      toast.success(t.product.removed);
      navigate(listUrl('/stock'), { replace: true });
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  if (query.isLoading) return <LoadingSkeleton count={2} />;
  if (query.isError) return <ErrorMessage message={getApiErrorMessage(query.error)} retry={() => query.refetch()} />;
  if (!query.data) return <EmptyState icon="🔍" title={t.common.notFound} description={t.product.notFound} />;

  const { product, batches, recentMovements, consumedByYear } = query.data;
  const year = currentYear();
  const thisYear = consumedByYear.find((c) => c.year === year)?.quantity ?? 0;
  const lastYear = consumedByYear.find((c) => c.year === year - 1);
  const smallButton =
    'rounded-md border border-gray-300 bg-white px-3 py-1.5 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50';

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader title={`${CATEGORY_ICONS[product.category]} ${product.name}`} backTo={listUrl('/stock')} backLabel={t.stock.title} />

      <section className="rounded-lg bg-white p-4 shadow-sm sm:p-5">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <p className="text-4xl font-bold text-gray-900">
              {product.quantity} <span className="text-base font-normal text-gray-500">szt.</span>
            </p>
            <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-gray-600">
              {product.quantity > 0 && <FreshnessBadge status={product.freshness} />}
              {product.nextExpiresOn && <span>{t.stock.nextExpiry(formatDate(product.nextExpiresOn))}</span>}
              {product.belowMinimum && <span className="font-medium text-blue-700">{t.product.belowMinimum(product.minQuantity - product.quantity)}</span>}
              {product.archived && <span className="rounded-full bg-gray-100 px-2 py-0.5 text-xs">{t.product.archived}</span>}
            </div>
          </div>
          <div className="flex flex-wrap gap-2">
            <Link
              to={`/add?productId=${product.id}`}
              className="rounded-md bg-blue-600 px-3 py-1.5 text-sm font-semibold text-white hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500"
            >
              {t.product.addBatch}
            </Link>
            <Link to={`/products/${product.id}/edit`} className={smallButton}>
              {t.common.edit}
            </Link>
            <button type="button" className={smallButton} disabled={archiveMutation.isPending} onClick={() => archiveMutation.mutate(!product.archived)}>
              {product.archived ? t.product.unarchive : t.product.archive}
            </button>
            <button type="button" className={`${smallButton} text-red-700 hover:bg-red-50`} onClick={() => setRemoving(true)}>
              {t.common.delete}
            </button>
          </div>
        </div>
        <dl className="mt-4 grid grid-cols-2 gap-x-4 gap-y-2 text-sm sm:grid-cols-3">
          <div>
            <dt className="text-gray-500">{t.product.kind}</dt>
            <dd className="font-medium text-gray-900">{t.product.kindLabel[product.kind]}</dd>
          </div>
          <div>
            <dt className="text-gray-500">{t.product.category}</dt>
            <dd className="font-medium text-gray-900">{t.product.categoryLabel[product.category]}</dd>
          </div>
          {product.packageSize && (
            <div>
              <dt className="text-gray-500">{t.product.packageSize}</dt>
              <dd className="font-medium text-gray-900">{product.packageSize}</dd>
            </div>
          )}
          {product.defaultShelfLifeMonths && (
            <div>
              <dt className="text-gray-500">{t.product.shelfLifeMonths}</dt>
              <dd className="font-medium text-gray-900">{product.defaultShelfLifeMonths}</dd>
            </div>
          )}
          {product.minQuantity > 0 && (
            <div>
              <dt className="text-gray-500">{t.product.minQuantity}</dt>
              <dd className="font-medium text-gray-900">{product.minQuantity}</dd>
            </div>
          )}
          {product.barcode && (
            <div>
              <dt className="text-gray-500">{t.product.barcode}</dt>
              <dd className="font-mono text-gray-900">{product.barcode}</dd>
            </div>
          )}
          <div>
            <dt className="text-gray-500">{t.product.consumedThisYear(thisYear, year).split(':')[0]}</dt>
            <dd className="font-medium text-gray-900">
              {thisYear}
              {lastYear && <span className="ml-2 text-gray-500">({t.product.consumedLastYear(lastYear.quantity, lastYear.year)})</span>}
            </dd>
          </div>
        </dl>
        {product.notes && <p className="mt-3 whitespace-pre-line text-sm text-gray-700">{product.notes}</p>}
      </section>

      <section className="mt-6" aria-labelledby="batches-heading">
        <h2 id="batches-heading" className="mb-2 text-lg font-semibold text-gray-900">
          {t.product.batchesSection}
        </h2>
        {batches.length === 0 ? (
          <p className="rounded-lg bg-white p-4 text-sm text-gray-500 shadow-sm">{t.product.noBatches}</p>
        ) : (
          <ul className="space-y-2">
            {batches.map((batch) => (
              <BatchCard
                key={batch.id}
                batch={batch}
                product={product}
                extra={
                  <Link to={`/batches/${batch.id}/edit?productId=${product.id}`} className="shrink-0 text-sm font-medium text-blue-600 hover:text-blue-700">
                    {t.common.edit}
                  </Link>
                }
              />
            ))}
          </ul>
        )}
      </section>

      <section className="mt-6" aria-labelledby="history-heading">
        <h2 id="history-heading" className="mb-2 text-lg font-semibold text-gray-900">
          {t.product.historySection}
        </h2>
        {recentMovements.length === 0 ? (
          <p className="rounded-lg bg-white p-4 text-sm text-gray-500 shadow-sm">{t.product.noHistory}</p>
        ) : (
          <ul className="divide-y divide-gray-100 rounded-lg bg-white px-4 shadow-sm">
            {recentMovements.map((m) => (
              <li key={m.id} className="flex items-center justify-between gap-3 py-2 text-sm">
                <span className="text-gray-700">
                  {t.movement.type[m.type]}
                  {m.userName && <span className="ml-1 text-gray-400">· {m.userName}</span>}
                </span>
                <span className="shrink-0">
                  <span className={`font-semibold ${m.quantity > 0 ? 'text-green-700' : 'text-gray-900'}`}>{m.quantity > 0 ? `+${m.quantity}` : m.quantity}</span>
                  <span className="ml-2 text-gray-400">{formatDate(m.occurredOn)}</span>
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>

      <ConfirmationModal
        isOpen={removing}
        onClose={() => setRemoving(false)}
        onConfirm={() => deleteMutation.mutate()}
        title={t.product.removeConfirmTitle}
        description={t.product.removeConfirmText}
        confirmText={t.common.delete}
        variant="danger"
        isLoading={deleteMutation.isPending}
      />
    </div>
  );
}
