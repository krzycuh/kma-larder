export type ProductKind = 'STORE_BOUGHT' | 'HOMEMADE';

export const PRODUCT_CATEGORIES = [
  'PRESERVES',
  'HONEY',
  'FLOUR_GRAINS',
  'PASTA_RICE',
  'CANNED',
  'BEVERAGES',
  'OILS_SAUCES',
  'SWEETS_SNACKS',
  'SPICES_BAKING',
  'DRIED',
  'FROZEN',
  'OTHER',
] as const;
export type ProductCategory = (typeof PRODUCT_CATEGORIES)[number];

export type FreshnessStatus = 'OK' | 'EXPIRING_SOON' | 'EXPIRED' | 'UNKNOWN';

export type MovementType = 'ADDED' | 'CONSUMED' | 'DISCARDED' | 'ADJUSTED';

/** A date as printed on packaging: a year, optionally a month, optionally a day. */
export interface ApproximateDate {
  year: number;
  month: number | null;
  day: number | null;
}

export interface Product {
  id: string;
  pantryId: string;
  name: string;
  kind: ProductKind;
  category: ProductCategory;
  barcode: string | null;
  packageSize: string | null;
  defaultShelfLifeMonths: number | null;
  minQuantity: number;
  notes: string | null;
  archived: boolean;
  /** Units currently in stock (sum over batches) */
  quantity: number;
  batchCount: number;
  /** Earliest best-before date among batches in stock (ISO date) */
  nextExpiresOn: string | null;
  /** Worst freshness among batches in stock */
  freshness: FreshnessStatus;
  belowMinimum: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface Batch {
  id: string;
  pantryId: string;
  productId: string;
  quantity: number;
  bestBefore: ApproximateDate | null;
  producedOn: ApproximateDate | null;
  /** Last day of the best-before period (given, or derived from producedOn + shelf life) */
  expiresOn: string | null;
  freshness: FreshnessStatus;
  /** Days until expiresOn (negative when past); null without a date */
  daysLeft: number | null;
  location: string | null;
  notes: string | null;
  addedOn: string;
  createdAt: string;
  updatedAt: string;
}

/** A batch together with its product, for lists that span many products (expiring, dashboard). */
export interface BatchWithProduct extends Batch {
  product: Product;
}

export interface StockMovement {
  id: string;
  pantryId: string;
  productId: string;
  productName: string;
  batchId: string | null;
  type: MovementType;
  /** Signed change of the batch quantity */
  quantity: number;
  occurredOn: string;
  userName: string | null;
  createdAt: string;
}

export interface ProductDetail {
  product: Product;
  batches: Batch[];
  recentMovements: StockMovement[];
  /** Units consumed per calendar year, newest first */
  consumedByYear: { year: number; quantity: number }[];
}

export interface ReplenishmentItem {
  product: Product;
  quantity: number;
  minQuantity: number;
  missing: number;
}

export interface Dashboard {
  pantryId: string;
  soonDays: number;
  productCount: number;
  unitCount: number;
  expiredCount: number;
  expiringSoonCount: number;
  belowMinimumCount: number;
  expiring: BatchWithProduct[];
  belowMinimum: ReplenishmentItem[];
  recentMovements: StockMovement[];
}

export type ProductSort = 'name' | 'expiry' | 'quantity';

export interface ProductListParams {
  search?: string;
  category?: ProductCategory;
  kind?: ProductKind;
  includeArchived?: boolean;
  sort?: Uppercase<ProductSort>;
  page?: number;
  pageSize?: number;
}

export interface ProductRequest {
  name: string;
  kind: ProductKind;
  category: ProductCategory;
  barcode?: string | null;
  packageSize?: string | null;
  defaultShelfLifeMonths?: number | null;
  minQuantity?: number;
  notes?: string | null;
  archived?: boolean;
}

export interface BatchRequest {
  quantity: number;
  bestBefore?: ApproximateDate | null;
  producedOn?: ApproximateDate | null;
  location?: string | null;
  notes?: string | null;
  addedOn?: string | null;
}

export interface UpdateBatchRequest {
  bestBefore?: ApproximateDate | null;
  producedOn?: ApproximateDate | null;
  location?: string | null;
  notes?: string | null;
}

export interface QuantityChangeRequest {
  quantity: number;
  occurredOn?: string | null;
}

export interface ExpiringParams {
  /** Only batches that expire within this many days (expired included); omit for all dated batches */
  withinDays?: number;
  /** true: only batches that have already expired */
  expiredOnly?: boolean;
  /** true: only batches without any date */
  undatedOnly?: boolean;
}

export interface BarcodeMetadata {
  barcode: string;
  name: string;
  brand: string | null;
  packageSize: string | null;
  imageUrl: string | null;
  source: string;
}

export interface BarcodeLookup {
  barcode: string;
  /** The product in this pantry that already has the barcode */
  product: Product | null;
  /** What an external database knows about the barcode */
  metadata: BarcodeMetadata | null;
}
