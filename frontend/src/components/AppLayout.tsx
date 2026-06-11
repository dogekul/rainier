import { NavLink, Outlet } from 'react-router-dom';
import { useAuthStore } from '../store/auth';
import './AppLayout.css';

interface NavItem {
  to: string;
  label: string;
}

interface NavGroup {
  key: string;
  title: string;
  items: NavItem[];
}

const navGroups: NavGroup[] = [
  {
    key: 'org',
    title: '组织',
    items: [
      { to: '/org/organizations', label: '组织节点' },
      { to: '/org/users', label: '用户' },
      { to: '/org/user-organizations', label: '用户-组织关系' },
    ],
  },
  {
    key: 'product',
    title: '产品',
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
    items: [
      { to: '/hr/positions', label: '岗位' },
      { to: '/hr/roles', label: '角色' },
      { to: '/hr/user-roles', label: '用户角色' },
    ],
  },
  {
    key: 'sys',
    title: '系统',
    items: [{ to: '/sys/audit-logs', label: '审计日志' }],
  },
];

/**
 * Page shell: header (brand + username) + left Sider nav + main content.
 *
 * <p>Sider menu groups are statically configured; a future change can swap to a permission-aware
 * generator hook.
 */
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
      <div className="rainier-shell-body">
        <aside className="rainier-shell-sider" data-testid="appshell-sider">
          {navGroups.map((g) => (
            <div key={g.key} className="rainier-shell-sider-group">
              <div className="rainier-shell-sider-group-title">{g.title}</div>
              {g.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) =>
                    `rainier-shell-sider-item${isActive ? ' rainier-shell-sider-item-active' : ''}`
                  }
                  data-testid={`appshell-nav-${item.to}`}
                >
                  {item.label}
                </NavLink>
              ))}
            </div>
          ))}
        </aside>
        <main className="rainier-shell-main">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
