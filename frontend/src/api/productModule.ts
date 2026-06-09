import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type ProductModuleStatus = 'PLANNING' | 'ACTIVE' | 'DEPRECATED';

export interface ProductModule {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  status: ProductModuleStatus;
  productId: number;
  productCode?: string | null;
  productName?: string | null;
  ownerUserId: number;
  ownerName?: string | null;
  ownerLoginName?: string | null;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface ProductModuleCreate {
  code: string;
  name: string;
  description?: string;
  status?: ProductModuleStatus;
  productId: number;
  ownerUserId: number;
}

export interface ProductModuleUpdate {
  code: string;
  name: string;
  description?: string;
  status: ProductModuleStatus;
  ownerUserId: number;
}

export interface ProductModuleListParams {
  productId?: number;
  status?: ProductModuleStatus;
  search?: string;
  page?: number;
  size?: number;
}

export async function listProductModules(
  params: ProductModuleListParams = {},
): Promise<PaginatedResult<ProductModule>> {
  const res = await client.get<PaginatedResult<ProductModule>>('/product-modules', {
    params,
  });
  return res.data;
}

export async function getProductModule(id: number): Promise<ProductModule> {
  const res = await client.get<ProductModule>(`/product-modules/${id}`);
  return res.data;
}

export async function createProductModule(
  body: ProductModuleCreate,
): Promise<ProductModule> {
  const res = await client.post<ProductModule>('/product-modules', body);
  return res.data;
}

export async function updateProductModule(
  id: number,
  body: ProductModuleUpdate,
): Promise<ProductModule> {
  const res = await client.put<ProductModule>(`/product-modules/${id}`, body);
  return res.data;
}

export async function deleteProductModule(id: number): Promise<void> {
  await client.delete(`/product-modules/${id}`);
}
