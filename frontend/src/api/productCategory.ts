import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type ProductCategoryStatus = 'ACTIVE' | 'ARCHIVED';

export interface ProductCategory {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  status: ProductCategoryStatus;
  ownerUserId: number;
  ownerName?: string | null;
  ownerLoginName?: string | null;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface ProductCategoryCreate {
  code: string;
  name: string;
  description?: string;
  status?: ProductCategoryStatus;
  ownerUserId: number;
}

export interface ProductCategoryUpdate {
  code: string;
  name: string;
  description?: string;
  status: ProductCategoryStatus;
  ownerUserId: number;
}

export interface ProductCategoryListParams {
  status?: ProductCategoryStatus;
  search?: string;
  page?: number;
  size?: number;
}

export async function listProductCategories(
  params: ProductCategoryListParams = {},
): Promise<PaginatedResult<ProductCategory>> {
  const res = await client.get<PaginatedResult<ProductCategory>>('/product-categories', {
    params,
  });
  return res.data;
}

export async function getProductCategory(id: number): Promise<ProductCategory> {
  const res = await client.get<ProductCategory>(`/product-categories/${id}`);
  return res.data;
}

export async function createProductCategory(
  body: ProductCategoryCreate,
): Promise<ProductCategory> {
  const res = await client.post<ProductCategory>('/product-categories', body);
  return res.data;
}

export async function updateProductCategory(
  id: number,
  body: ProductCategoryUpdate,
): Promise<ProductCategory> {
  const res = await client.put<ProductCategory>(`/product-categories/${id}`, body);
  return res.data;
}

export async function deleteProductCategory(id: number): Promise<void> {
  await client.delete(`/product-categories/${id}`);
}
