import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../store/auth';

/**
 * Gate for authenticated routes. Redirects to {@code /login} when the auth store has no token.
 * Covers spec frontend-scaffold → "未登录访问首页时重定向".
 */
export function ProtectedRoute() {
  const token = useAuthStore((s) => s.token);
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return <Outlet />;
}
