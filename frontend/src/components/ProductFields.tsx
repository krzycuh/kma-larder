import { t } from '../i18n';
import { PRODUCT_CATEGORIES, type ProductCategory, type ProductKind } from '../types/stock';
import { CATEGORY_ICONS } from '../utils/categories';
import { ProductThumb } from './ProductThumb';

import { inputClasses } from '../utils/formStyles';
import type { ProductFieldsValue, ProductFormErrors } from '../utils/productForm';

interface ProductFieldsProps {
  value: ProductFieldsValue;
  onChange: (value: ProductFieldsValue) => void;
  errors?: ProductFormErrors;
  /** Rendered next to the barcode field (scanner button) */
  barcodeAction?: React.ReactNode;
  /** Show the longer set of fields (edit page) or the short one (quick add) */
  full?: boolean;
}

/**
 * Everything that describes a product apart from its name: kind, category, barcode, package, shelf life, minimum.
 */
export function ProductFields({ value, onChange, errors = {}, barcodeAction, full = true }: ProductFieldsProps) {
  const set = <K extends keyof ProductFieldsValue>(key: K, v: ProductFieldsValue[K]) => onChange({ ...value, [key]: v });
  const kinds: ProductKind[] = ['STORE_BOUGHT', 'HOMEMADE'];

  return (
    <div className="space-y-4">
      <div>
        <span className="block text-sm font-medium text-gray-700">{t.product.kind}</span>
        <div className="mt-1 grid grid-cols-2 gap-2" role="radiogroup" aria-label={t.product.kind}>
          {kinds.map((kind) => {
            const checked = value.kind === kind;
            return (
              <button
                key={kind}
                type="button"
                role="radio"
                aria-checked={checked}
                onClick={() => set('kind', kind)}
                className={`rounded-lg border px-3 py-2 text-left text-sm focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                  checked ? 'border-blue-500 bg-blue-50 text-blue-800' : 'border-gray-300 bg-white text-gray-700 hover:bg-gray-50'
                }`}
              >
                <span className="block font-semibold">{t.product.kindLabel[kind]}</span>
                <span className="block text-xs opacity-80">{t.product.kindHint[kind]}</span>
              </button>
            );
          })}
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <div>
          <label htmlFor="product-category" className="block text-sm font-medium text-gray-700">
            {t.product.category}
          </label>
          <select id="product-category" value={value.category} onChange={(e) => set('category', e.target.value as ProductCategory)} className={inputClasses}>
            {PRODUCT_CATEGORIES.map((category) => (
              <option key={category} value={category}>
                {CATEGORY_ICONS[category]} {t.product.categoryLabel[category]}
              </option>
            ))}
          </select>
        </div>
        <div>
          <label htmlFor="product-package" className="block text-sm font-medium text-gray-700">
            {t.product.packageSize} <span className="text-gray-400">({t.common.optional})</span>
          </label>
          <input
            id="product-package"
            type="text"
            value={value.packageSize}
            onChange={(e) => set('packageSize', e.target.value)}
            placeholder={t.product.packageSizePlaceholder}
            className={inputClasses}
          />
        </div>
      </div>

      {value.kind === 'HOMEMADE' && (
        <div>
          <label htmlFor="product-shelf-life" className="block text-sm font-medium text-gray-700">
            {t.product.shelfLifeMonths} <span className="text-gray-400">({t.common.optional})</span>
          </label>
          <input
            id="product-shelf-life"
            inputMode="numeric"
            value={value.defaultShelfLifeMonths}
            onChange={(e) => set('defaultShelfLifeMonths', e.target.value.replace(/\D/g, '').slice(0, 3))}
            className={`${inputClasses} sm:w-40`}
          />
          <p className="mt-1 text-xs text-gray-500">{t.product.shelfLifeHint}</p>
          {errors.defaultShelfLifeMonths && <p className="mt-1 text-sm text-red-700">{errors.defaultShelfLifeMonths}</p>}
        </div>
      )}

      {(full || value.kind === 'STORE_BOUGHT') && (
        <div>
          <label htmlFor="product-barcode" className="block text-sm font-medium text-gray-700">
            {t.product.barcode} <span className="text-gray-400">({t.product.barcodeHint})</span>
          </label>
          <div className="mt-1 flex gap-2">
            <input
              id="product-barcode"
              inputMode="numeric"
              value={value.barcode}
              onChange={(e) => set('barcode', e.target.value.replace(/\D/g, '').slice(0, 14))}
              className={`${inputClasses} mt-0`}
            />
            {barcodeAction}
          </div>
          {errors.barcode && <p className="mt-1 text-sm text-red-700">{errors.barcode}</p>}
        </div>
      )}

      {full && (
        <>
          <div>
            <label htmlFor="product-min" className="block text-sm font-medium text-gray-700">
              {t.product.minQuantity}
            </label>
            <input
              id="product-min"
              inputMode="numeric"
              value={value.minQuantity}
              onChange={(e) => set('minQuantity', e.target.value.replace(/\D/g, '').slice(0, 5))}
              className={`${inputClasses} sm:w-40`}
            />
            <p className="mt-1 text-xs text-gray-500">{t.product.minQuantityHint}</p>
          </div>
          <div>
            <label htmlFor="product-image" className="block text-sm font-medium text-gray-700">
              {t.product.imageUrl} <span className="text-gray-400">({t.common.optional})</span>
            </label>
            <div className="mt-1 flex items-start gap-3">
              <ProductThumb product={{ category: value.category, imageUrl: value.imageUrl.trim() || null }} size="md" />
              <div className="min-w-0 flex-1">
                <input
                  id="product-image"
                  type="url"
                  inputMode="url"
                  value={value.imageUrl}
                  onChange={(e) => set('imageUrl', e.target.value)}
                  placeholder={t.product.imageUrlPlaceholder}
                  className={`${inputClasses} mt-0`}
                />
                <p className="mt-1 text-xs text-gray-500">
                  {t.product.imageUrlHint}
                  {value.imageUrl && (
                    <>
                      {' · '}
                      <button type="button" className="underline hover:text-gray-900" onClick={() => set('imageUrl', '')}>
                        {t.product.removeImage}
                      </button>
                    </>
                  )}
                </p>
                {errors.imageUrl && <p className="mt-1 text-sm text-red-700">{errors.imageUrl}</p>}
              </div>
            </div>
          </div>
          <div>
            <label htmlFor="product-notes" className="block text-sm font-medium text-gray-700">
              {t.product.notes} <span className="text-gray-400">({t.common.optional})</span>
            </label>
            <textarea
              id="product-notes"
              rows={3}
              value={value.notes}
              onChange={(e) => set('notes', e.target.value)}
              placeholder={t.product.notesPlaceholder}
              className={inputClasses}
            />
          </div>
        </>
      )}
    </div>
  );
}
