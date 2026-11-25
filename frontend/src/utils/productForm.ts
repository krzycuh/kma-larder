import type { Product, ProductCategory, ProductKind, ProductRequest } from '../types/stock';
import { normalizeBarcode } from './barcode';
import { t } from '../i18n';

export interface ProductFieldsValue {
  kind: ProductKind;
  category: ProductCategory;
  barcode: string;
  packageSize: string;
  defaultShelfLifeMonths: string;
  minQuantity: string;
  notes: string;
}

export const DEFAULT_PRODUCT_FIELDS: ProductFieldsValue = {
  kind: 'STORE_BOUGHT',
  category: 'OTHER',
  barcode: '',
  packageSize: '',
  defaultShelfLifeMonths: '',
  minQuantity: '0',
  notes: '',
};

export type ProductFormErrors = Partial<Record<keyof ProductFieldsValue | 'name', string>>;

export function productToFields(product: Product): ProductFieldsValue {
  return {
    kind: product.kind,
    category: product.category,
    barcode: product.barcode ?? '',
    packageSize: product.packageSize ?? '',
    defaultShelfLifeMonths: product.defaultShelfLifeMonths ? String(product.defaultShelfLifeMonths) : '',
    minQuantity: String(product.minQuantity),
    notes: product.notes ?? '',
  };
}

export interface ProductFormResult {
  request?: ProductRequest;
  errors: ProductFormErrors;
}

/** Validates the name and fields and builds the API request; the barcode is normalised (UPC-A → EAN-13). */
export function buildProductRequest(name: string, fields: ProductFieldsValue, archived = false): ProductFormResult {
  const errors: ProductFormResult['errors'] = {};
  const trimmedName = name.trim();
  if (!trimmedName) errors.name = t.product.nameRequired;
  let barcode: string | null = null;
  if (fields.barcode.trim()) {
    barcode = normalizeBarcode(fields.barcode);
    if (!barcode) errors.barcode = t.product.invalidBarcode;
  }
  const shelfLife = fields.kind === 'HOMEMADE' && fields.defaultShelfLifeMonths ? Number(fields.defaultShelfLifeMonths) : null;
  if (shelfLife !== null && (shelfLife < 1 || shelfLife > 600)) errors.defaultShelfLifeMonths = t.product.shelfLifeMonths;
  if (Object.keys(errors).length > 0) return { errors };
  return {
    errors,
    request: {
      name: trimmedName,
      kind: fields.kind,
      category: fields.category,
      barcode,
      packageSize: fields.packageSize.trim() || null,
      defaultShelfLifeMonths: shelfLife,
      minQuantity: Number(fields.minQuantity || 0),
      notes: fields.notes.trim() || null,
      archived,
    },
  };
}
