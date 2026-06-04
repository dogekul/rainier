import { Outlet } from 'react-router-dom';
import { useAuthStore } from '../store/auth';
import './AppLayout.css';

/** Page shell with header (brand + username top-right) and main content slot. */
export function AppLayout() {
  const user = useAuthStore((s) => s.user);
  return (
    <div className="rainier-shell">
      <header className="rainier-shell-header">
        <span className="rainier-shell-brand">Rainier</span>
        <span className="rainier-shell-user" data-testid="appshell-username">
          {user?.username ?? ''}
        </span>
      </header>
      <main className="rainier-shell-main">
        <Outlet />
      </main>
    </div>
  );
}
