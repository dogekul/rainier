import { useEffect, useState } from 'react';
import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { me } from '../api/auth';
import { isElevated, useAuthStore } from '../store/auth';

/**
 * Admin-only path prefixes (v0.0.20). NOTE: the 产品 group lives under `/pm/...` alongside the
 * all-users 需求管理 group, so we list the three product paths explicitly rather than a blanket
 * `/pm` — otherwise non-admins would be bounced from `/pm/projects` etc.
 */
const ADMIN_PATH_PREFIXES = [
  '/org',
  '/hr',
  '/sys',
  '/pm/products',
  '/pm/product-modules',
  '/pm/features',
];

export function isAdminPath(pathname: string): boolean {
  return ADMIN_PATH_PREFIXES.some((p) => pathname === p || pathname.startsWith(p + '/'));
}

/**
 * Gate for authenticated routes. Redirects to `/login` when the auth store has no token.
 *
 * <p>v0.0.20: also the app-level current-user hydration point — on entry it calls `GET /api/auth/me`
 * once (when role context is missing) and writes id/name/roles/projects into the store, so role-scoped
 * navigation and route guards work everywhere. Non-admin users hitting an admin route are redirected
 * home — but only AFTER hydration, so an admin isn't briefly bounced on the very first paint.
 */
export function ProtectedRoute() {
  const token = useAuthStore((s) => s.token);
  const user = useAuthStore((s) => s.user);
  const setAuth = useAuthStore((s) => s.setAuth);
  const location = useLocation();
  // Already have role context (e.g. in-app navigation after hydration) → guard immediately.
  const [hydrated, setHydrated] = useState(() => user?.roles != null);

  useEffect(() => {
    if (!token) return;
    if (user?.roles != null) {
      setHydrated(true);
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const res = await me();
        if (cancelled) return;
        setAuth(token, {
          username: res.username,
          id: res.id,
          name: res.name,
          roles: res.roles,
          projects: res.projects,
          defaultLandingPath: res.defaultLandingPath || '/',
        });
      } catch {
        // A 401 is already handled by the axios interceptor (→ /login); any other error leaves the
        // user un-hydrated-but-marked, so the guard treats them as non-admin rather than hanging.
      } finally {
        if (!cancelled) setHydrated(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [token, user?.roles, setAuth]);

  if (!token) {
    return <Navigate to="/login" replace />;
  }
  if (hydrated && isAdminPath(location.pathname) && !isElevated(user)) {
    return <Navigate to="/" replace />;
  }
  if (hydrated && location.pathname === '/' && user?.defaultLandingPath && user.defaultLandingPath !== '/') {
    return <Navigate to={user.defaultLandingPath} replace />;
  }
  return <Outlet />;
}
