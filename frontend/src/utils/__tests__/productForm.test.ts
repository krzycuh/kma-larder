import { describe, expect, it } from 'vitest';
import { buildProductRequest, DEFAULT_PRODUCT_FIELDS, isValidImageUrl, productToFields } from '../productForm';
import type { Product } from '../../types/stock';

const product: Product = {
  id: 'p1',
  pantryId: 'a',
  name: 'Pomidory krojone',
  kind: 'STORE_BOUGHT',
  category: 'CANNED',
  barcode: '5900020022745',
  packageSize: '400 g',
  defaultShelfLifeMonths: null,
  minQuantity: 2,
  notes: null,
  imageUrl: 'https://images.openfoodfacts.org/images/products/590/002/002/2745/front_pl.4.200.jpg',
  archived: false,
  quantity: 3,
  batchCount: 1,
  nextExpiresOn: null,
  freshness: 'UNKNOWN',
  belowMinimum: false,
  createdAt: '2026-01-01T00:00:00',
  updatedAt: '2026-01-01T00:00:00',
};

describe('productForm image link', () => {
  it('accepts http(s) links without whitespace and rejects the rest', () => {
    expect(isValidImageUrl('https://example.org/a.jpg')).toBe(true);
    expect(isValidImageUrl('http://example.org/a.jpg')).toBe(true);
    expect(isValidImageUrl('javascript:alert(1)')).toBe(false);
    expect(isValidImageUrl('https://example.org/a b.jpg')).toBe(false);
    expect(isValidImageUrl('https://' + 'a'.repeat(500))).toBe(false);
  });

  it('round-trips the link through the form fields', () => {
    const fields = productToFields(product);
    expect(fields.imageUrl).toBe(product.imageUrl);
    const result = buildProductRequest(product.name, fields);
    expect(result.errors).toEqual({});
    expect(result.request?.imageUrl).toBe(product.imageUrl);
    expect(result.request?.category).toBe('CANNED');
  });

  it('sends null for an empty link and an error for a bad one', () => {
    expect(buildProductRequest('Woda', { ...DEFAULT_PRODUCT_FIELDS, imageUrl: '  ' }).request?.imageUrl).toBeNull();
    const bad = buildProductRequest('Woda', { ...DEFAULT_PRODUCT_FIELDS, imageUrl: 'ftp://x/y.jpg' });
    expect(bad.request).toBeUndefined();
    expect(bad.errors.imageUrl).toBeTruthy();
  });
});
