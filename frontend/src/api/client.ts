import axios, { AxiosError } from 'axios';
import { useAuthStore } from '../store/auth';

/**
 * Project-wide HTTP client.
 *
 * <p>baseURL is {@code /api} so both dev (vite proxy) and prod (nginx reverse proxy) share the
 * same code path; backend CORS config (see backend {@code CorsConfig}) covers both origins.
 *
 * <p>Request interceptor: injects {@code Authorization: Bearer <token>} when the store has one.
 * Response interceptor: on HTTP 401, clears the auth store and redirects to {@code /login}.
 */
const client = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: { 'Content-Type': 'application/json' },
});

client.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  },
);

export default client;
