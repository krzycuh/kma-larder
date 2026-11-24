import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useMutation, useQuery } from '@tanstack/react-query';
import type { AxiosError } from 'axios';
import { pantryStockApi, productsApi } from '../api/stock';
import { PageHeader } from '../components/PageHeader';
import { PantrySwitcher } from '../components/PantrySwitcher';
import { LoadingSpinner } from '../components/LoadingSpinner';
import { EmptyState } from '../components/EmptyState';
import { ProductPicker } from '../components/ProductPicker';
import { ProductFields } from '../components/ProductFields';
import { inputClasses } from '../utils/formStyles';
import { ApproximateDateInput } from '../components/ApproximateDateInput';
import { QuantityStepper } from '../components/QuantityStepper';
import { BarcodeScanner } from '../components/BarcodeScanner';
import { NavIcon } from '../components/NavIcon';
import { useCurrentPantry } from '../hooks/usePantries';
import { useInvalidateStock } from '../hooks/useStockMutations';
import { useToast } from '../hooks/useToast';
import { t } from '../i18n';
import type { BatchRequest, Product } from '../types/stock';
import { getApiErrorMessage } from '../utils/apiError';
import { EMPTY_DATE_INPUT, isPast, parseDateInput, type ApproximateDateInputValue } from '../utils/approximateDate';
import { buildProductRequest, DEFAULT_PRODUCT_FIELDS, type ProductFieldsValue, type ProductFormErrors } from '../utils/productForm';
import { normalizeBarcode } from '../utils/barcode';

type FormErrors = ProductFormErrors & {
  quantity?: string;
  bestBefore?: string;
  producedOn?: string;
};

/**
 * The main entry flow: name (existing product or a new one), how many, until when – and it is on the shelf.
 * ?productId= preselects a product, ?scan=1 opens the barcode scanner right away.
 */
export function AddStock() {
  const navigate = useNavigate();
  const toast = useToast();
  const invalidate = useInvalidateStock();
  const [searchParams, setSearchParams] = useSearchParams();
  const prefillId = searchParams.get('productId');
  const { pantry, isLoading: pantryLoading } = useCurrentPantry();

  const [name, setName] = useState('');
  const [selected, setSelected] = useState<Product | null>(null);
  const [fields, setFields] = useState<ProductFieldsValue>(DEFAULT_PRODUCT_FIELDS);
  const [quantity, setQuantity] = useState(1);
  const [bestBefore, setBestBefore] = useState<ApproximateDateInputValue>(EMPTY_DATE_INPUT);
  const [producedOn, setProducedOn] = useState<ApproximateDateInputValue>(EMPTY_DATE_INPUT);
  const [location, setLocation] = useState('');
  const [errors, setErrors] = useState<FormErrors>({});
  const [scannerOpen, setScannerOpen] = useState(searchParams.get('scan') === '1');
  const [addAnother, setAddAnother] = useState(false);

  const prefill = useQuery({ queryKey: ['product', prefillId], queryFn: () => productsApi.get(prefillId!), enabled: !!prefillId });
  useEffect(() => {
    if (prefill.data) {
      setSelected(prefill.data.product);
      setName(prefill.data.product.name);
    }
  }, [prefill.data]);

  const kind = selected?.kind ?? fields.kind;

  const reset = () => {
    setName('');
    setSelected(null);
    setFields(DEFAULT_PRODUCT_FIELDS);
    setQuantity(1);
    setBestBefore(EMPTY_DATE_INPUT);
    setProducedOn(EMPTY_DATE_INPUT);
    setLocation('');
    setErrors({});
    if (prefillId) setSearchParams({}, { replace: true });
  };

  const mutation = useMutation({
    mutationFn: async ({ product, batch }: { product: Product | null; batch: BatchRequest }) => {
      let target = product;
      if (!target) {
        const result = buildProductRequest(name, fields);
        target = await pantryStockApi.createProduct(pantry!.id, result.request!);
      }
      await productsApi.addBatch(target.id, batch);
      return target;
    },
    onSuccess: (product) => {
      invalidate();
      toast.success(t.batch.added);
      if (addAnother) reset();
      else navigate(`/products/${product.id}`);
    },
    onError: (err) => toast.error(getApiErrorMessage(err)),
  });

  const submit = (another: boolean) => {
    const nextErrors: FormErrors = {};
    if (!selected) Object.assign(nextErrors, buildProductRequest(name, fields).errors);
    if (quantity < 1) nextErrors.quantity = t.batch.quantityRequired;
    const bb = parseDateInput(bestBefore);
    const po = parseDateInput(producedOn);
    if (bb.error) nextErrors.bestBefore = t.batch.invalidDate;
    if (po.error) nextErrors.producedOn = t.batch.invalidDate;
    setErrors(nextErrors);
    if (Object.keys(nextErrors).length > 0) return;
    setAddAnother(another);
    mutation.mutate({
      product: selected,
      batch: { quantity, bestBefore: bb.date, producedOn: kind === 'HOMEMADE' ? po.date : null, location: location.trim() || null },
    });
  };

  const [lookupHint, setLookupHint] = useState<string | null>(null);
  const lookup = useMutation({
    mutationFn: (barcode: string) => pantryStockApi.lookupBarcode(pantry!.id, barcode),
    onSuccess: (result) => {
      if (result.product) {
        setSelected(result.product);
        setName(result.product.name);
        setLookupHint(t.barcode.foundInPantry);
        return;
      }
      if (result.metadata) {
        const meta = result.metadata;
        setSelected(null);
        setName(meta.brand && !meta.name.toLowerCase().includes(meta.brand.toLowerCase()) ? `${meta.name} ${meta.brand}` : meta.name);
        setFields((f) => ({ ...f, barcode: meta.barcode, kind: 'STORE_BOUGHT', packageSize: f.packageSize || meta.packageSize || '' }));
        setLookupHint(t.barcode.found(t.barcode.sourceName[meta.source] ?? meta.source));
      }
    },
    onError: (err) => {
      const status = (err as AxiosError)?.response?.status;
      setLookupHint(status === 404 ? t.barcode.notFound : status === 503 ? t.barcode.unavailable : status === 400 ? t.barcode.invalid : getApiErrorMessage(err));
    },
  });

  const lookupBarcode = (raw: string) => {
    const barcode = normalizeBarcode(raw);
    if (!barcode) {
      setLookupHint(t.barcode.invalid);
      return;
    }
    setSelected(null);
    setFields((f) => ({ ...f, barcode, kind: 'STORE_BOUGHT' }));
    setLookupHint(null);
    lookup.mutate(barcode);
  };

  const onScanned = (barcode: string) => {
    setScannerOpen(false);
    if (searchParams.get('scan')) setSearchParams({}, { replace: true });
    lookupBarcode(barcode);
  };

  if (pantryLoading || (prefillId && prefill.isLoading)) return <LoadingSpinner />;
  if (!pantry) return <EmptyState icon="🫙" title={t.dashboard.emptyPantryTitle} description={t.pantry.noPantry} />;

  const parsedBestBefore = parseDateInput(bestBefore).date;
  const scanButton = (
    <>
      <button
        type="button"
        onClick={() => setScannerOpen(true)}
        className="inline-flex shrink-0 items-center gap-1.5 rounded-md border border-gray-300 bg-white px-3 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-blue-500"
        aria-label={t.scanner.openLabel}
      >
        <NavIcon name="camera" className="h-4 w-4" />
        {t.scanner.open}
      </button>
      {fields.barcode.length >= 8 && (
        <button
          type="button"
          onClick={() => lookupBarcode(fields.barcode)}
          disabled={lookup.isPending}
          className="shrink-0 rounded-md border border-blue-600 bg-white px-3 py-2 text-sm font-medium text-blue-700 hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50"
        >
          {lookup.isPending ? t.barcode.looking : t.barcode.lookup}
        </button>
      )}
    </>
  );

  return (
    <div className="mx-auto max-w-2xl">
      <PageHeader title={t.addStock.title} description={t.addStock.subtitle} backTo="/" backLabel={t.nav.home} />
      <form
        onSubmit={(e) => {
          e.preventDefault();
          submit(false);
        }}
        className="space-y-6"
      >
        <section className="space-y-4 rounded-lg bg-white p-4 shadow-sm sm:p-6" aria-labelledby="product-section">
          <div className="flex items-center justify-between gap-3">
            <h2 id="product-section" className="text-base font-semibold text-gray-900">
              {t.addStock.productSection}
            </h2>
            <PantrySwitcher />
          </div>
          <ProductPicker
            pantryId={pantry.id}
            name={name}
            selected={selected}
            onChange={(n, p) => {
              setName(n);
              setSelected(p);
            }}
            error={errors.name}
            autoFocus={!prefillId}
          />
          {lookupHint && (
            <p className="rounded-md bg-blue-50 px-3 py-2 text-sm text-blue-800" role="status">
              {lookupHint}
            </p>
          )}
          {!selected && <ProductFields value={fields} onChange={setFields} errors={errors} full={false} barcodeAction={scanButton} />}
          {selected && (
            <p className="text-sm text-gray-600">
              {t.product.kindLabel[selected.kind]} · {t.product.categoryLabel[selected.category]}
              {selected.packageSize ? ` · ${selected.packageSize}` : ''} · {t.product.inStock(selected.quantity)}
            </p>
          )}
        </section>

        <section className="space-y-4 rounded-lg bg-white p-4 shadow-sm sm:p-6" aria-labelledby="batch-section">
          <h2 id="batch-section" className="text-base font-semibold text-gray-900">
            {t.addStock.batchSection}
          </h2>
          <div>
            <label htmlFor="batch-quantity" className="block text-sm font-medium text-gray-700">
              {t.batch.quantity}
            </label>
            <div className="mt-1">
              <QuantityStepper id="batch-quantity" value={quantity} onChange={setQuantity} min={1} label={t.batch.quantity} />
            </div>
            {errors.quantity && <p className="mt-1 text-sm text-red-700">{errors.quantity}</p>}
          </div>
          {kind === 'HOMEMADE' && (
            <ApproximateDateInput id="produced-on" label={t.batch.producedOn} hint={t.batch.producedOnHint} value={producedOn} onChange={setProducedOn} error={errors.producedOn} />
          )}
          <ApproximateDateInput
            id="best-before"
            label={t.batch.bestBefore}
            hint={kind === 'HOMEMADE' ? t.batch.precisionHint : t.batch.bestBeforeHint}
            value={bestBefore}
            onChange={setBestBefore}
            error={errors.bestBefore ?? (parsedBestBefore && isPast(parsedBestBefore) ? t.batch.inPast : null)}
          />
          <div>
            <label htmlFor="batch-location" className="block text-sm font-medium text-gray-700">
              {t.batch.location} <span className="text-gray-400">({t.common.optional})</span>
            </label>
            <input id="batch-location" type="text" value={location} onChange={(e) => setLocation(e.target.value)} placeholder={t.batch.locationPlaceholder} className={inputClasses} />
          </div>
        </section>

        <div className="flex flex-col-reverse gap-3 sm:flex-row sm:justify-end">
          <button type="button" onClick={() => navigate(-1)} className="rounded-lg border border-gray-300 px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50">
            {t.common.cancel}
          </button>
          <button
            type="button"
            disabled={mutation.isPending}
            onClick={() => submit(true)}
            className="rounded-lg border border-blue-600 bg-white px-4 py-2 text-sm font-semibold text-blue-700 hover:bg-blue-50 disabled:opacity-50"
          >
            {t.addStock.addAnother}
          </button>
          <button type="submit" disabled={mutation.isPending} className="rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50">
            {mutation.isPending ? t.addStock.adding : t.addStock.submit}
          </button>
        </div>
      </form>

      <BarcodeScanner open={scannerOpen} onClose={() => setScannerOpen(false)} onDetected={onScanned} />
    </div>
  );
}
