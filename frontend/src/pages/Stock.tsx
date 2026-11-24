import { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { pantryStockApi } from '../api/stock';
import { PageHeader } from '../components/PageHeader';
import { PantrySwitcher } from '../components/PantrySwitcher';
import { EmptyState } from '../components/EmptyState';
import { ErrorMessage } from '../components/ErrorMessage';
import { LoadingSkeleton } from '../components/LoadingSkeleton';
import { Pagination } from '../components/Pagination';
import { FreshnessBadge } from '../components/FreshnessBadge';
import { useCurrentPantry } from '../hooks/usePantries';
import { useDebouncedSearchInput, useListSearchParams } from '../hooks/useListSearchParams';
import { t } from '../i18n';
import { PRODUCT_CATEGORIES, type ProductCategory, type ProductKind, type ProductSort } from '../types/stock';
import { formatDate } from '../utils/format';
import { CATEGORY_ICONS } from '../utils/categories';

const PAGE_SIZE = 50;
const SORTS: ProductSort[] = ['name', 'expiry', 'quantity'];
const selectClasses =
  'rounded-md border border-gray-300 bg-white px-3 py-2 text-sm text-gray-900 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500';

/**
 * Stock: every product with how many units are on the shelves, filtered by category and kind.
 */
export function Stock() {
  const navigate = useNavigate();
  const { pantry, isLoading: pantryLoading, isError: pantryError } = useCurrentPantry();
  const { get, page, setFilters, setPage, clampPage } = useListSearchParams();
  const debouncedSearch = get('q') ?? '';
  const [search, setSearch] = useDebouncedSearchInput(debouncedSearch, (q) => setFilters({ q }));
  const category = PRODUCT_CATEGORIES.find((c) => c === get('category')) as ProductCategory | undefined;
  const kind = (['STORE_BOUGHT', 'HOMEMADE'] as ProductKind[]).find((k) => k === get('kind'));
  const sort = SORTS.find((s) => s === get('sort')) ?? 'name';
  const includeArchived = get('archived') === '1';

  const params = {
    search: debouncedSearch || undefined,
    category,
    kind,
    sort: sort.toUpperCase() as Uppercase<ProductSort>,
    includeArchived: includeArchived || undefined,
    page,
    pageSize: PAGE_SIZE,
  };
  const productsQuery = useQuery({
    queryKey: ['products', pantry?.id, params],
    queryFn: () => pantryStockApi.products(pantry!.id, params),
    enabled: !!pantry,
  });

  const totalPages = productsQuery.data?.totalPages;
  useEffect(() => clampPage(totalPages), [clampPage, totalPages]);
  const hasFilters = !!(debouncedSearch || category || kind || includeArchived);

  return (
    <div className="mx-auto max-w-5xl">
      <PageHeader
        title={t.stock.title}
        description={t.stock.subtitle}
        actions={
          <Link
            to="/add"
            className="inline-flex items-center rounded-lg bg-blue-600 px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
          >
            + {t.stock.add}
          </Link>
        }
      />

      <div className="mb-4">
        <PantrySwitcher />
      </div>

      <div className="mb-4 grid grid-cols-1 gap-2 sm:grid-cols-4">
        <label className="sr-only" htmlFor="stock-search">
          {t.common.search}
        </label>
        <input
          id="stock-search"
          type="search"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={t.common.searchPlaceholder}
          className="w-full rounded-md border border-gray-300 px-3 py-2 text-base text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500 sm:col-span-2"
        />
        <select
          aria-label={t.product.category}
          value={category ?? ''}
          onChange={(e) => setFilters({ category: e.target.value || undefined })}
          className={selectClasses}
        >
          <option value="">{t.stock.allCategories}</option>
          {PRODUCT_CATEGORIES.map((c) => (
            <option key={c} value={c}>
              {CATEGORY_ICONS[c]} {t.product.categoryLabel[c]}
            </option>
          ))}
        </select>
        <select aria-label={t.product.kind} value={kind ?? ''} onChange={(e) => setFilters({ kind: e.target.value || undefined })} className={selectClasses}>
          <option value="">{t.stock.allKinds}</option>
          <option value="STORE_BOUGHT">{t.product.kindLabel.STORE_BOUGHT}</option>
          <option value="HOMEMADE">{t.product.kindLabel.HOMEMADE}</option>
        </select>
      </div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-2 text-sm">
        <div className="inline-flex rounded-lg bg-gray-100 p-1" role="tablist" aria-label={t.stock.sortBy}>
          {SORTS.map((s) => (
            <button
              key={s}
              type="button"
              role="tab"
              aria-selected={sort === s}
              onClick={() => setFilters({ sort: s === 'name' ? undefined : s })}
              className={`rounded-md px-3 py-1.5 font-medium transition-colors focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                sort === s ? 'bg-white text-blue-700 shadow-sm' : 'text-gray-600 hover:text-gray-900'
              }`}
            >
              {t.stock.sort[s]}
            </button>
          ))}
        </div>
        <label className="flex items-center gap-2 text-gray-600">
          <input type="checkbox" checked={includeArchived} onChange={(e) => setFilters({ archived: e.target.checked ? '1' : undefined })} />
          {t.stock.showArchived}
        </label>
      </div>

      {pantryLoading || (pantry && productsQuery.isLoading) ? (
        <LoadingSkeleton count={3} />
      ) : pantryError || productsQuery.isError ? (
        <ErrorMessage message={t.common.errorGeneric} retry={() => productsQuery.refetch()} />
      ) : !pantry ? (
        <EmptyState icon="🫙" title={t.stock.emptyTitle} description={t.pantry.noPantry} />
      ) : productsQuery.data && productsQuery.data.items.length === 0 ? (
        hasFilters ? (
          <EmptyState icon="🔍" title={t.common.notFound} description={t.stock.emptyFiltered} />
        ) : (
          <EmptyState icon="🫙" title={t.stock.emptyTitle} description={t.stock.emptyText} action={{ label: t.stock.add, onClick: () => navigate('/add') }} />
        )
      ) : (
        <>
          <p className="mb-2 text-sm text-gray-500">{t.stock.productsCount(productsQuery.data?.totalItems ?? 0)}</p>
          <ul className="grid grid-cols-1 gap-2 md:grid-cols-2">
            {productsQuery.data?.items.map((product) => (
              <li key={product.id}>
                <Link
                  to={`/products/${product.id}`}
                  className={`flex items-center gap-3 rounded-lg bg-white p-3 shadow-sm transition-colors hover:bg-blue-50 focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                    product.archived ? 'opacity-60' : ''
                  }`}
                >
                  <span className="text-2xl" aria-hidden="true">
                    {CATEGORY_ICONS[product.category]}
                  </span>
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-semibold text-gray-900">
                      {product.name}
                      {product.packageSize && <span className="ml-1 text-sm font-normal text-gray-500">{product.packageSize}</span>}
                    </p>
                    <p className="flex flex-wrap items-center gap-x-2 text-xs text-gray-500">
                      <span>{t.product.kindLabel[product.kind]}</span>
                      {product.nextExpiresOn && <span>{t.stock.nextExpiry(formatDate(product.nextExpiresOn))}</span>}
                      {product.belowMinimum && <span className="font-medium text-blue-700">{t.product.belowMinimum(product.minQuantity - product.quantity)}</span>}
                      {product.archived && <span>{t.product.archived}</span>}
                    </p>
                  </div>
                  <div className="flex flex-col items-end gap-1">
                    <span className={`text-2xl font-bold ${product.quantity === 0 ? 'text-gray-400' : 'text-gray-900'}`}>{product.quantity}</span>
                    {product.quantity > 0 && <FreshnessBadge status={product.freshness} />}
                  </div>
                </Link>
              </li>
            ))}
          </ul>
          <Pagination page={page} totalPages={productsQuery.data?.totalPages ?? 0} onChange={setPage} />
        </>
      )}
    </div>
  );
}
