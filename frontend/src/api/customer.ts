import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export interface Customer {
  id: number;
  name: string;
  industry?: string | null;
  contactName?: string | null;
  notes?: string | null;
  createTime?: string;
  updateTime?: string;
}

export interface CustomerCreate {
  name: string;
  industry?: string;
  contactName?: string;
  notes?: string;
}

export type CustomerUpdate = CustomerCreate;

export interface CustomerListParams {
  search?: string;
  page?: number;
  size?: number;
}

export async function listCustomers(
  params: CustomerListParams = {},
): Promise<PaginatedResult<Customer>> {
  const res = await client.get<PaginatedResult<Customer>>('/customers', { params });
  return res.data;
}

export async function getCustomer(id: number): Promise<Customer> {
  const res = await client.get<Customer>(`/customers/${id}`);
  return res.data;
}

export async function createCustomer(body: CustomerCreate): Promise<Customer> {
  const res = await client.post<Customer>('/customers', body);
  return res.data;
}

export async function updateCustomer(id: number, body: CustomerUpdate): Promise<Customer> {
  const res = await client.put<Customer>(`/customers/${id}`, body);
  return res.data;
}

export async function deleteCustomer(id: number): Promise<void> {
  await client.delete(`/customers/${id}`);
}
