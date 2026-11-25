/**
 * Retail barcodes (GTIN): EAN-13, EAN-8, UPC-A (12 digits, stored as EAN-13 with a leading zero).
 * A scanner may report a 2- or 5-digit add-on glued to an EAN-13; it is dropped.
 */
function gtinChecksumValid(digits: string): boolean {
  let sum = 0;
  for (let i = 0; i < digits.length - 1; i++) {
    const weight = (digits.length - 1 - i) % 2 === 1 ? 3 : 1;
    sum += Number(digits[i]) * weight;
  }
  return (10 - (sum % 10)) % 10 === Number(digits[digits.length - 1]);
}

/** Normalises a typed or scanned code to a valid 8- or 13-digit GTIN, or null. */
export function normalizeBarcode(rawValue: string): string | null {
  const digits = rawValue.replace(/\D/g, '');
  let code: string | null = null;
  if (digits.length === 8) code = digits;
  else if (digits.length === 12) code = `0${digits}`;
  else if ([13, 15, 18].includes(digits.length)) code = digits.slice(0, 13);
  if (!code) return null;
  return gtinChecksumValid(code) ? code : null;
}

/** Scanned value → product barcode, or null when it is not a retail code (or a misread). */
export function barcodeFromScan(rawValue: string): string | null {
  return normalizeBarcode(rawValue);
}

export function isValidBarcode(value: string): boolean {
  return normalizeBarcode(value) !== null;
}
