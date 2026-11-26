import { useEffect, useId, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { pantryStockApi } from '../api/stock';
import { t } from '../i18n';
import type { Product } from '../types/stock';
import { ProductThumb } from './ProductThumb';

interface ProductPickerProps {
  pantryId: string;
  /** The typed name (also the name of a new product) */
  name: string;
  /** The chosen existing product, or null for a new one */
  selected: Product | null;
  onChange: (name: string, selected: Product | null) => void;
  error?: string | null;
  autoFocus?: boolean;
}

/**
 * Name field with suggestions from the pantry: pick an existing product, or keep typing to create one.
 */
export function ProductPicker({ pantryId, name, selected, onChange, error, autoFocus }: ProductPickerProps) {
  const listId = useId();
  const [open, setOpen] = useState(false);
  const [debounced, setDebounced] = useState(name);
  const wrapperRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handle = setTimeout(() => setDebounced(name.trim()), 200);
    return () => clearTimeout(handle);
  }, [name]);

  const suggestions = useQuery({
    queryKey: ['product-search', pantryId, debounced],
    queryFn: () => pantryStockApi.searchProducts(pantryId, debounced),
    enabled: !selected && debounced.length >= 2,
    staleTime: 30_000,
  });

  useEffect(() => {
    const onClick = (event: MouseEvent) => {
      if (!wrapperRef.current?.contains(event.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', onClick);
    return () => document.removeEventListener('mousedown', onClick);
  }, []);

  const items = suggestions.data ?? [];
  const showList = open && !selected && debounced.length >= 2;

  return (
    <div ref={wrapperRef} className="relative">
      <label htmlFor={`${listId}-input`} className="block text-sm font-medium text-gray-700">
        {t.product.name}
      </label>
      <div className="mt-1 flex gap-2">
        <input
          id={`${listId}-input`}
          type="text"
          autoFocus={autoFocus}
          autoComplete="off"
          role="combobox"
          aria-expanded={showList}
          aria-controls={`${listId}-list`}
          aria-autocomplete="list"
          value={name}
          placeholder={t.product.namePlaceholder}
          onFocus={() => setOpen(true)}
          onChange={(e) => {
            onChange(e.target.value, null);
            setOpen(true);
          }}
          className="block w-full rounded-md border border-gray-300 px-3 py-2 text-base text-gray-900 placeholder-gray-400 focus:border-blue-500 focus:outline-none focus:ring-1 focus:ring-blue-500"
        />
      </div>
      <p className="mt-1 text-xs text-gray-500">
        {selected ? (
          <>
            <span className="font-medium text-green-700">{t.product.existingProduct}</span>
            {' · '}
            <button type="button" className="underline hover:text-gray-900" onClick={() => onChange(name, null)}>
              {t.common.edit}
            </button>
          </>
        ) : debounced.length >= 2 && suggestions.isSuccess && items.length === 0 ? (
          <span className="font-medium text-blue-700">{t.product.noSuggestions}</span>
        ) : (
          t.product.pickExisting
        )}
      </p>
      {error && <p className="mt-1 text-sm text-red-700">{error}</p>}
      {showList && items.length > 0 && (
        <ul
          id={`${listId}-list`}
          role="listbox"
          className="absolute z-20 mt-1 max-h-64 w-full overflow-auto rounded-md border border-gray-200 bg-white py-1 shadow-lg"
        >
          {items.map((product) => (
            <li key={product.id} role="option" aria-selected={false}>
              <button
                type="button"
                onClick={() => {
                  onChange(product.name, product);
                  setOpen(false);
                }}
                className="flex w-full items-center gap-2 px-3 py-2 text-left hover:bg-blue-50 focus:bg-blue-50 focus:outline-none"
              >
                <ProductThumb product={product} className="h-8 w-8" />
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-medium text-gray-900">{product.name}</span>
                  <span className="block truncate text-xs text-gray-500">
                    {t.product.categoryLabel[product.category]}
                    {product.packageSize ? ` · ${product.packageSize}` : ''} · {t.stock.quantityShort(product.quantity)}
                  </span>
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
