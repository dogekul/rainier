import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type ProductStatus = 'PLANNING' | 'ACTIVE' | 'SUNSET' | 'ARCHIVED';

export interface Product {
  id: number;
  code: string;
  name: string;
  description?: string | null;
  status: ProductStatus;
  categoryId: number;
  categoryCode?: string | null;
  categoryName?: string | null;
  ownerUserId: number;
  ownerName?: string | null;
  ownerLoginName?: string | null;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface ProductCreate {
  code: string;
  name: string;
  description?: string;
  status?: ProductStatus;
  categoryId: number;
  ownerUserId: number;
}

export interface ProductUpdate {
  code: string;
  name: string;
  description?: string;
  status: ProductStatus;
  ownerUserId: number;
}

export interface ProductListParams {
  categoryId?: number;
  status?: ProductStatus;
  search?: string;
  page?: number;
  size?: number;
}

export async function listProducts(
  params: ProductListParams = {},
): Promise<PaginatedResult<Product>> {
  const res = await client.get<PaginatedResult<Product>>('/products', { params });
  return res.data;
}

export async function getProduct(id: number): Promise<Product> {
  const res = await client.get<Product>(`/products/${id}`);
  return res.data;
}

export async function createProduct(body: ProductCreate): Promise<Product> {
  const res = await client.post<Product>('/products', body);
  return res.data;
}

export async function updateProduct(id: number, body: ProductUpdate): Promise<Product> {
  const res = await client.put<Product>(`/products/${id}`, body);
  return res.data;
}

export async function deleteProduct(id: number): Promise<void> {
  await client.delete(`/products/${id}`);
}
