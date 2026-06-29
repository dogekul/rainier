import { useState } from 'react';
import { Link, NavLink, Outlet } from 'react-router-dom';
import { isElevated, useAuthStore } from '../store/auth';
import { AiErrorOverdueBanner } from './AiErrorOverdueBanner';
import { NavIcon } from './NavIcon';
import { NotificationBell } from './NotificationBell';
import './AppLayout.css';

export interface NavItem {
  to: string;
  label: string;
  /** react-router NavLink `end` — needed for "/" so it isn't active on every route. */
  end?: boolean;
  /** v0.0.34 — NavIcon name. */
  icon?: string;
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
    items: [
      { to: '/', label: '我的工作台', end: true, icon: 'home' },
      { to: '/profile', label: '我的档案', icon: 'badge' },
      { to: '/inbox', label: '需求收件箱', icon: 'inbox' },
      { to: '/demand-submit', label: '提个诉求', icon: 'edit' },
      { to: '/team', label: '团队负责人面板', icon: 'users' },
    ],
  },
  {
    key: 'dashboards',
    title: '数据看板',
    items: [
      { to: '/portfolio', label: '项目地图', icon: 'map' },
      { to: '/reviews', label: '评审看板', icon: 'check' },
      { to: '/metrics', label: '度量看板', icon: 'gauge' },
    ],
  },
  {
    key: 'pmo',
    title: 'PMO',
    items: [
      { to: '/pmo', label: '公司项目地图', icon: 'map' },
    ],
  },
  {
    key: 'crm',
    title: '客户',
    items: [
      { to: '/crm/customers', label: '客户', icon: 'users' },
      { to: '/crm/opportunities', label: '商机看板', icon: 'gauge' },
      { to: '/crm/presale-flow', label: '售前流转', icon: 'edit' },
      { to: '/crm/delivery-flow', label: '实施流转', icon: 'check' },
      { to: '/crm/operations', label: '运营看板', icon: 'loop' },
    ],
  },
  {
    key: 'ai',
    title: 'AI',
    items: [
      { to: '/ai/work-logs', label: 'AI 工作日志', icon: 'loop' },
      { to: '/ai/errors', label: '错误公示板', icon: 'shield' },
    ],
  },
  {
    key: 'org',
    title: '组织',
    requiresAdmin: true,
    items: [
      { to: '/org/organizations', label: '组织节点', icon: 'sitemap' },
      { to: '/org/users', label: '用户', icon: 'user' },
      { to: '/org/user-organizations', label: '用户-组织关系', icon: 'link' },
    ],
  },
  {
    key: 'product',
    title: '产品',
    requiresAdmin: true,
    items: [
      { to: '/pm/products', label: '产品', icon: 'box' },
      { to: '/pm/product-modules', label: '产品模块', icon: 'layers' },
      { to: '/pm/features', label: '功能', icon: 'star' },
    ],
  },
  {
    key: 'pm',
    title: '需求管理',
    items: [
      { to: '/pm/cockpit', label: '项目驾驶舱', icon: 'gauge' },
      { to: '/pm/projects', label: '项目', icon: 'folder' },
      { to: '/pm/sprints', label: 'Sprint', icon: 'loop' },
      { to: '/pm/tasks', label: '任务', icon: 'check' },
      { to: '/pm/demands', label: '诉求', icon: 'inbox' },
      { to: '/pm/requirements', label: '需求', icon: 'doc' },
      { to: '/pm/demand-requirements', label: '诉求-需求关联', icon: 'link' },
    ],
  },
  {
    key: 'hr',
    title: '人事配置',
    requiresAdmin: true,
    items: [
      { to: '/hr/positions', label: '岗位', icon: 'badge' },
      { to: '/hr/roles', label: '角色', icon: 'key' },
      { to: '/hr/user-roles', label: '用户角色', icon: 'user' },
    ],
  },
  {
    key: 'sys',
    title: '系统',
    requiresAdmin: true,
    items: [
      { to: '/sys/audit-logs', label: '审计日志', icon: 'shield' },
      { to: '/sys/compliance', label: '合规仪表盘', icon: 'gauge' },
    ],
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
      <AiErrorOverdueBanner />
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
        <div className="rainier-shell-header-right">
          <NotificationBell />
          <span className="rainier-shell-user" data-testid="appshell-username">
            {user?.username ?? ''}
          </span>
        </div>
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
                        <NavIcon name={item.icon} />
                        <span className="rainier-shell-sider-item-label">{item.label}</span>
                      </NavLink>
                    ))}
                </div>
              );
            })}
          </aside>
        )}
        <main className="rainier-shell-main">
          <div className="rainier-shell-content">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  );
}
