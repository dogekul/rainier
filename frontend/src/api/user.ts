import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export interface User {
  id: number;
  loginName: string;
  name: string;
  code?: string | null;
  emailAddress?: string | null;
  isInternal: boolean;
  enabled: boolean;
  delFlag?: boolean;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface UserCreate {
  loginName: string;
  name: string;
  code?: string;
  emailAddress?: string;
  isInternal?: boolean;
  enabled?: boolean;
}

export interface UserUpdate {
  name: string;
  code?: string;
  emailAddress?: string;
  isInternal?: boolean;
  enabled?: boolean;
}

export interface UserListParams {
  search?: string;
  isInternal?: boolean;
  enabled?: boolean;
  page?: number;
  size?: number;
}

export async function listUsers(params: UserListParams = {}): Promise<PaginatedResult<User>> {
  const res = await client.get<PaginatedResult<User>>('/users', { params });
  return res.data;
}

export async function getUser(id: number): Promise<User> {
  const res = await client.get<User>(`/users/${id}`);
  return res.data;
}

export async function createUser(body: UserCreate): Promise<User> {
  const res = await client.post<User>('/users', body);
  return res.data;
}

export async function updateUser(id: number, body: UserUpdate): Promise<User> {
  const res = await client.put<User>(`/users/${id}`, body);
  return res.data;
}

export async function deleteUser(id: number): Promise<void> {
  await client.delete(`/users/${id}`);
}
