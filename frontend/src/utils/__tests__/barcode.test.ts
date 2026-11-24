import { describe, expect, it } from 'vitest';
import { barcodeFromScan, isValidBarcode, normalizeBarcode } from '../barcode';

describe('normalizeBarcode', () => {
  it('accepts EAN-13 and EAN-8 codes with a valid check digit', () => {
    expect(normalizeBarcode('5901234123457')).toBe('5901234123457');
    expect(normalizeBarcode('590 1234 12345 7')).toBe('5901234123457');
    expect(normalizeBarcode('96385074')).toBe('96385074');
  });

  it('stores UPC-A as EAN-13 with a leading zero', () => {
    expect(normalizeBarcode('036000291452')).toBe('0036000291452');
  });

  it('drops a 2- or 5-digit add-on reported together with the code', () => {
    expect(barcodeFromScan('590123412345712')).toBe('5901234123457');
    expect(barcodeFromScan('590123412345712345')).toBe('5901234123457');
  });

  it('rejects wrong checksums and partial reads', () => {
    expect(normalizeBarcode('5901234123456')).toBeNull();
    expect(normalizeBarcode('590123412345')).toBeNull();
    expect(normalizeBarcode('')).toBeNull();
    expect(isValidBarcode('abc')).toBe(false);
    expect(isValidBarcode('5901234123457')).toBe(true);
  });
});
