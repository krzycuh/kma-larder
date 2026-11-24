import { useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { batchesApi, productsApi } from '../api/stock';
import { PageHeader } from '../components/PageHeader';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { EmptyState } from '../components/EmptyState';
import { ApproximateDateInput } from '../components/ApproximateDateInput';
import { inputClasses } from '../utils/formStyles';
import { useInvalidateStock } from '../hooks/useStockMutations';
import { useToast } from '../hooks/useToast';
import { t } from '../i18n';
import type { UpdateBatchRequest } from '../types/stock';
import { getApiErrorMessage } from '../utils/apiError';
import { EMPTY_DATE_INPUT, parseDateInput, toDateInput, type ApproximateDateInputValue } from '../utils/approximateDate';

/**
 * Edit a batch's dates, place and note. The quantity is changed on the product page (consume / adjust).
 * Route: /batches/:batchId/edit?productId= – the batch is read from the product's detail.
 */
export function BatchForm() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useToast();
  const invalidate = useInvalidateStock();
  const [searchParams] = useSearchParams();
  const productId = searchParams.get('productId');

  const [bestBefore, setBestBefore] = useState<ApproximateDateInputValue>(EMPTY_DATE_INPUT);
  const [producedOn, setProducedOn] = useState<ApproximateDateInputValue>(EMPTY_DATE_INPUT);
  const [location, setLocation] = useState('');
  const [notes, setNotes] = useState('');
  const [errors, setErrors] = useState<{ bestBefore?: string; producedOn?: string }>({});

  const query = useQuery({ queryKey: ['product', productId], queryFn: () => productsApi.get(productId!), enabled: !!productId });
  const batch = query.data?.batches.find((b) => b.id === id);

  useEffect(() => {
    if (batch) {
      setBestBefore(toDateInput(batch.bestBefore));
      setProducedOn(toDateInput(batch.producedOn));
      setLocation(batch.location ?? '');
      setNotes(batch.notes ?? '');
    }
  }, [batch]);

  const mutation = useMutation({
    mutationFn: (request: UpdateBatchRequest) => batchesApi.update(id!, request),
    onSuccess: () => {
      invalidate();
      toast.success(t.batch.updated);
      navigate(`/products/${productId}`);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const bb = parseDateInput(bestBefore);
    const po = parseDateInput(producedOn);
    const nextErrors = { bestBefore: bb.error ? t.batch.invalidDate : undefined, producedOn: po.error ? t.batch.invalidDate : undefined };
    setErrors(nextErrors);
    if (bb.error || po.error) return;
    mutation.mutate({ bestBefore: bb.date, producedOn: po.date, location: location.trim() || null, notes: notes.trim() || null });
  };

  if (!productId) return <EmptyState icon="🔍" title={t.common.notFound} description={t.product.notFound} />;
  if (query.isLoading) return <LoadingSpinner />;
  if (query.isError) return <ErrorMessage message={getApiErrorMessage(query.error)} retry={() => query.refetch()} />;
  if (!batch || !query.data) return <EmptyState icon="🔍" title={t.common.notFound} description={t.product.notFound} />;

  const product = query.data.product;
  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title={t.batch.editTitle} description={`${product.name} · ${t.stock.quantityShort(batch.quantity)}`} backTo={`/products/${productId}`} backLabel={product.name} />
      <form onSubmit={submit} className="space-y-5 rounded-lg bg-white p-4 shadow-sm sm:p-6">
        <ApproximateDateInput id="best-before" label={t.batch.bestBefore} hint={t.batch.bestBeforeHint} value={bestBefore} onChange={setBestBefore} error={errors.bestBefore} />
        {product.kind === 'HOMEMADE' && (
          <ApproximateDateInput id="produced-on" label={t.batch.producedOn} hint={t.batch.producedOnHint} value={producedOn} onChange={setProducedOn} error={errors.producedOn} />
        )}
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="batch-location" className="block text-sm font-medium text-gray-700">
              {t.batch.location} <span className="text-gray-400">({t.common.optional})</span>
            </label>
            <input id="batch-location" type="text" value={location} onChange={(e) => setLocation(e.target.value)} placeholder={t.batch.locationPlaceholder} className={inputClasses} />
          </div>
          <div>
            <label htmlFor="batch-notes" className="block text-sm font-medium text-gray-700">
              {t.batch.notes} <span className="text-gray-400">({t.common.optional})</span>
            </label>
            <input id="batch-notes" type="text" value={notes} onChange={(e) => setNotes(e.target.value)} className={inputClasses} />
          </div>
        </div>
        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button type="button" onClick={() => navigate(`/products/${productId}`)} className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50">
            {t.common.cancel}
          </button>
          <button type="submit" disabled={mutation.isPending} className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50">
            {mutation.isPending ? t.common.saving : t.common.save}
          </button>
        </div>
      </form>
    </div>
  );
}
