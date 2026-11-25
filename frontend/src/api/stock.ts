import apiClient from './client';
import type { PagedResponse } from '../types/common';
import type {
  BarcodeLookup,
  Batch,
  BatchRequest,
  BatchWithProduct,
  Dashboard,
  ExpiringParams,
  Product,
  ProductDetail,
  ProductListParams,
  ProductRequest,
  QuantityChangeRequest,
  ReplenishmentItem,
  StockMovement,
  UpdateBatchRequest,
} from '../types/stock';

/** Stock of one pantry: product list, lists of what to use up and what to buy, history, dashboard */
export const pantryStockApi = {
  products: async (pantryId: string, params: ProductListParams = {}): Promise<PagedResponse<Product>> => {
    const response = await apiClient.get<PagedResponse<Product>>(`/pantries/${pantryId}/products`, { params });
    return response.data;
  },

  searchProducts: async (pantryId: string, q: string, limit = 10): Promise<Product[]> => {
    const response = await apiClient.get<Product[]>(`/pantries/${pantryId}/products/search`, { params: { q, limit } });
    return response.data;
  },

  /** A scanned or typed barcode: the pantry's product with that code and/or what Open Food Facts knows */
  lookupBarcode: async (pantryId: string, barcode: string): Promise<BarcodeLookup> => {
    const response = await apiClient.get<BarcodeLookup>(`/pantries/${pantryId}/products/lookup`, { params: { barcode } });
    return response.data;
  },

  createProduct: async (pantryId: string, data: ProductRequest): Promise<Product> => {
    const response = await apiClient.post<Product>(`/pantries/${pantryId}/products`, data);
    return response.data;
  },

  expiring: async (pantryId: string, params: ExpiringParams = {}): Promise<BatchWithProduct[]> => {
    const response = await apiClient.get<BatchWithProduct[]>(`/pantries/${pantryId}/expiring`, { params });
    return response.data;
  },

  replenishment: async (pantryId: string): Promise<ReplenishmentItem[]> => {
    const response = await apiClient.get<ReplenishmentItem[]>(`/pantries/${pantryId}/replenishment`);
    return response.data;
  },

  movements: async (
    pantryId: string,
    params: { productId?: string; page?: number; pageSize?: number } = {},
  ): Promise<PagedResponse<StockMovement>> => {
    const response = await apiClient.get<PagedResponse<StockMovement>>(`/pantries/${pantryId}/movements`, { params });
    return response.data;
  },

  dashboard: async (pantryId: string): Promise<Dashboard> => {
    const response = await apiClient.get<Dashboard>(`/pantries/${pantryId}/dashboard`);
    return response.data;
  },
};

export const productsApi = {
  get: async (id: string): Promise<ProductDetail> => {
    const response = await apiClient.get<ProductDetail>(`/products/${id}`);
    return response.data;
  },

  update: async (id: string, data: ProductRequest): Promise<Product> => {
    const response = await apiClient.put<Product>(`/products/${id}`, data);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/products/${id}`);
  },

  addBatch: async (id: string, data: BatchRequest): Promise<Batch> => {
    const response = await apiClient.post<Batch>(`/products/${id}/batches`, data);
    return response.data;
  },
};

export const batchesApi = {
  update: async (id: string, data: UpdateBatchRequest): Promise<Batch> => {
    const response = await apiClient.put<Batch>(`/batches/${id}`, data);
    return response.data;
  },

  consume: async (id: string, data: QuantityChangeRequest): Promise<Batch> => {
    const response = await apiClient.post<Batch>(`/batches/${id}/consume`, data);
    return response.data;
  },

  discard: async (id: string, data: QuantityChangeRequest): Promise<Batch> => {
    const response = await apiClient.post<Batch>(`/batches/${id}/discard`, data);
    return response.data;
  },

  adjust: async (id: string, data: QuantityChangeRequest): Promise<Batch> => {
    const response = await apiClient.post<Batch>(`/batches/${id}/adjust`, data);
    return response.data;
  },

  delete: async (id: string): Promise<void> => {
    await apiClient.delete(`/batches/${id}`);
  },
};
