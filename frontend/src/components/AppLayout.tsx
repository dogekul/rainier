import { useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { isElevated, useAuthStore } from '../store/auth';
import './AppLayout.css';

export interface NavItem {
  to: string;
  label: string;
  /** react-router NavLink `end` — needed for "/" so it isn't active on every route. */
  end?: boolean;
}

export interface NavGroup {
  key: string;
  title: string;
  items: NavItem[];
  /** v0.0.20: only shown to elevated (admin) users. Plain users see just 工作台 + 需求管理. */
  requiresAdmin?: boolean;
}

/**
 * Single source of truth for the Sider nav. Exported so the ProtectedRoute admin-route guard can be
 * cross-checked against it (see navGuardConsistency.test) — the guard's path prefixes MUST stay in
 * sync with which groups are `requiresAdmin`.
 */
export const navGroups: NavGroup[] = [
  {
    key: 'workbench',
    title: '工作台',
    items: [{ to: '/', label: '我的工作台', end: true }],
  },
  {
    key: 'org',
    title: '组织',
    requiresAdmin: true,
    items: [
      { to: '/org/organizations', label: '组织节点' },
      { to: '/org/users', label: '用户' },
      { to: '/org/user-organizations', label: '用户-组织关系' },
    ],
  },
  {
    key: 'product',
    title: '产品',
    requiresAdmin: true,
    items: [
      { to: '/pm/products', label: '产品' },
      { to: '/pm/product-modules', label: '产品模块' },
      { to: '/pm/features', label: '功能' },
    ],
  },
  {
    key: 'pm',
    title: '需求管理',
    items: [
      { to: '/pm/projects', label: '项目' },
      { to: '/pm/sprints', label: 'Sprint' },
      { to: '/pm/tasks', label: '任务' },
      { to: '/pm/demands', label: '诉求' },
      { to: '/pm/requirements', label: '需求' },
      { to: '/pm/demand-requirements', label: '诉求-需求关联' },
    ],
  },
  {
    key: 'hr',
    title: '人事配置',
    requiresAdmin: true,
    items: [
      { to: '/hr/positions', label: '岗位' },
      { to: '/hr/roles', label: '角色' },
      { to: '/hr/user-roles', label: '用户角色' },
    ],
  },
  {
    key: 'sys',
    title: '系统',
    requiresAdmin: true,
    items: [{ to: '/sys/audit-logs', label: '审计日志' }],
  },
];

/**
 * Page shell: header (sider toggle + brand link + username) + collapsible left Sider nav + main
 * content. v0.0.18: brand links home, a 工作台 group fronts the nav, each group folds, and the whole
 * Sider can be collapsed.
 */
export function AppLayout() {
  const user = useAuthStore((s) => s.user);
  const [collapsedGroups, setCollapsedGroups] = useState<Set<string>>(new Set());
  const [siderCollapsed, setSiderCollapsed] = useState(false);

  // v0.0.20: plain users see only the all-users groups (工作台 + 需求管理); admins see all six.
  const elevated = isElevated(user);
  const visibleGroups = navGroups.filter((g) => !g.requiresAdmin || elevated);

  const toggleGroup = (key: string) =>
    setCollapsedGroups((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });

  return (
    <div className="rainier-shell">
      <header className="rainier-shell-header">
        <div className="rainier-shell-header-left">
          <button
            type="button"
            className="rainier-shell-sider-toggle"
            onClick={() => setSiderCollapsed((c) => !c)}
            data-testid="appshell-sider-toggle"
            aria-label="收起或展开侧边栏"
            aria-expanded={!siderCollapsed}
          >
            ☰
          </button>
          <Link to="/" className="rainier-shell-brand" data-testid="appshell-brand">
            Rainier
          </Link>
        </div>
        <span className="rainier-shell-user" data-testid="appshell-username">
          {user?.username ?? ''}
        </span>
      </header>
      <div className="rainier-shell-body">
        {!siderCollapsed && (
          <aside className="rainier-shell-sider" data-testid="appshell-sider">
            {visibleGroups.map((g) => {
              const collapsed = collapsedGroups.has(g.key);
              return (
                <div key={g.key} className="rainier-shell-sider-group">
                  <button
                    type="button"
                    className="rainier-shell-sider-group-title"
                    onClick={() => toggleGroup(g.key)}
                    data-testid={`appshell-group-${g.key}`}
                    aria-expanded={!collapsed}
                  >
                    <span
                      className={`rainier-shell-sider-caret${collapsed ? ' is-collapsed' : ''}`}
                      aria-hidden="true"
                    >
                      ▾
                    </span>
                    <span className="rainier-shell-sider-group-label">{g.title}</span>
                  </button>
                  {!collapsed &&
                    g.items.map((item) => (
                      <NavLink
                        key={item.to}
                        to={item.to}
                        end={item.end}
                        className={({ isActive }) =>
                          `rainier-shell-sider-item${isActive ? ' rainier-shell-sider-item-active' : ''}`
                        }
                        data-testid={`appshell-nav-${item.to}`}
                      >
                        {item.label}
                      </NavLink>
                    ))}
                </div>
              );
            })}
          </aside>
        )}
        <main className="rainier-shell-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
