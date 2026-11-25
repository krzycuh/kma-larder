import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import { productsApi } from '../api/stock';
import { PageHeader } from '../components/PageHeader';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { ErrorMessage } from '../components/ErrorMessage';
import { ProductFields } from '../components/ProductFields';
import { inputClasses } from '../utils/formStyles';
import { useInvalidateStock } from '../hooks/useStockMutations';
import { useToast } from '../hooks/useToast';
import { t } from '../i18n';
import { getApiErrorMessage } from '../utils/apiError';
import { buildProductRequest, DEFAULT_PRODUCT_FIELDS, productToFields, type ProductFieldsValue, type ProductFormErrors, type ProductFormResult } from '../utils/productForm';

/**
 * Edit a product's description (name, kind, category, barcode, shelf life, minimum, notes).
 */
export function ProductForm() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const toast = useToast();
  const invalidate = useInvalidateStock();
  const [name, setName] = useState('');
  const [fields, setFields] = useState<ProductFieldsValue>(DEFAULT_PRODUCT_FIELDS);
  const [archived, setArchived] = useState(false);
  const [errors, setErrors] = useState<ProductFormErrors>({});

  const query = useQuery({ queryKey: ['product', id], queryFn: () => productsApi.get(id!), enabled: !!id });

  useEffect(() => {
    if (query.data) {
      setName(query.data.product.name);
      setFields(productToFields(query.data.product));
      setArchived(query.data.product.archived);
    }
  }, [query.data]);

  const mutation = useMutation({
    mutationFn: (request: NonNullable<ProductFormResult['request']>) => productsApi.update(id!, request),
    onSuccess: () => {
      invalidate();
      toast.success(t.product.updated);
      navigate(`/products/${id}`);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    const result = buildProductRequest(name, fields, archived);
    setErrors(result.errors);
    if (result.request) mutation.mutate(result.request);
  };

  if (query.isLoading) return <LoadingSpinner />;
  if (query.isError) return <ErrorMessage message={getApiErrorMessage(query.error)} retry={() => query.refetch()} />;

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title={t.product.editTitle} backTo={`/products/${id}`} backLabel={query.data?.product.name ?? t.common.back} />
      <form onSubmit={submit} className="space-y-5 rounded-lg bg-white p-4 shadow-sm sm:p-6">
        <div>
          <label htmlFor="product-name" className="block text-sm font-medium text-gray-700">
            {t.product.name}
          </label>
          <input id="product-name" type="text" value={name} onChange={(e) => setName(e.target.value)} className={inputClasses} />
          {errors.name && <p className="mt-1 text-sm text-red-700">{errors.name}</p>}
        </div>
        <ProductFields value={fields} onChange={setFields} errors={errors} full />
        <label className="flex items-start gap-2 text-sm text-gray-700">
          <input type="checkbox" checked={archived} onChange={(e) => setArchived(e.target.checked)} className="mt-1" />
          <span>
            <span className="font-medium">{t.product.archived}</span>
            <span className="block text-xs text-gray-500">{t.product.archivedHint}</span>
          </span>
        </label>
        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button
            type="button"
            onClick={() => navigate(`/products/${id}`)}
            className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50"
          >
            {t.common.cancel}
          </button>
          <button
            type="submit"
            disabled={mutation.isPending}
            className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {mutation.isPending ? t.common.saving : t.common.save}
          </button>
        </div>
      </form>
    </div>
  );
}
