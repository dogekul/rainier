import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import type { MeRole } from '../api/auth';
import { useAuthStore } from '../store/auth';
import { AppLayout } from './AppLayout';

function role(adminAccess: boolean): MeRole {
  return {
    roleId: 1,
    roleCode: 'R',
    roleName: 'R',
    projectId: null,
    projectName: null,
    projectCode: null,
    adminAccess,
  };
}

describe('AppLayout Sider (TC-FES-201)', () => {
  beforeEach(() => {
    // v0.0.20: default to an admin user so the existing full-console group assertions hold.
    useAuthStore.setState({
      token: 'tk',
      user: { username: 'alice', roles: [role(true)] },
    });
  });

  it('renders organization menu group with 3 items', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/" element={<div>home</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByTestId('appshell-sider')).toBeInTheDocument();
    expect(screen.getByText('组织')).toBeInTheDocument();
    expect(screen.getByText('组织节点')).toBeInTheDocument();
    expect(screen.getByText('用户')).toBeInTheDocument();
    expect(screen.getByText('用户-组织关系')).toBeInTheDocument();
    expect(screen.getByTestId('appshell-nav-/org/organizations')).toHaveAttribute(
      'href',
      '/org/organizations',
    );
  });

  /** TC-FES-D01 (v0.0.8 retrofit / TC-FES-P01): Sider 含「需求管理」菜单组 + 4 子项 + 项目排第一。 */
  it('renders the 需求管理 menu group with 4 items (项目 first) (TC-FES-D01 / TC-FES-P01)', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/" element={<div>home</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText('需求管理')).toBeInTheDocument();
    expect(screen.getByText('项目')).toBeInTheDocument();
    expect(screen.getByText('诉求')).toBeInTheDocument();
    expect(screen.getByText('需求')).toBeInTheDocument();
    expect(screen.getByText('诉求-需求关联')).toBeInTheDocument();
    expect(screen.getByTestId('appshell-nav-/pm/projects')).toHaveAttribute(
      'href',
      '/pm/projects',
    );
    expect(screen.getByTestId('appshell-nav-/pm/demands')).toHaveAttribute(
      'href',
      '/pm/demands',
    );
    // 项目项 SHALL 位于诉求项之前.
    const sider = screen.getByTestId('appshell-sider');
    const html = sider.innerHTML;
    expect(html.indexOf('/pm/projects')).toBeLessThan(html.indexOf('/pm/demands'));
  });

  /** TC-FES-TSK-001 (v0.0.11): Sider 「需求管理」 6 项 + 任务 位于 Sprint 与 诉求 之间. */
  it('renders the 需求管理 group with 6 items in v0.0.11 (任务 third) (TC-FES-TSK-001)', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/" element={<div>home</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );
    // 6 items present
    expect(screen.getByText('项目')).toBeInTheDocument();
    expect(screen.getByText('Sprint')).toBeInTheDocument();
    expect(screen.getByText('任务')).toBeInTheDocument();
    expect(screen.getByText('诉求')).toBeInTheDocument();
    expect(screen.getByText('需求')).toBeInTheDocument();
    expect(screen.getByText('诉求-需求关联')).toBeInTheDocument();
    expect(screen.getByTestId('appshell-nav-/pm/tasks')).toHaveAttribute(
      'href',
      '/pm/tasks',
    );
    // Order: 项目 → Sprint → 任务 → 诉求 → 需求 → 关联.
    const sider = screen.getByTestId('appshell-sider');
    const html = sider.innerHTML;
    expect(html.indexOf('/pm/projects')).toBeLessThan(html.indexOf('/pm/sprints'));
    expect(html.indexOf('/pm/sprints')).toBeLessThan(html.indexOf('/pm/tasks'));
    expect(html.indexOf('/pm/tasks')).toBeLessThan(html.indexOf('/pm/demands'));
  });

  /** TC-FES-PROD-001 (v0.0.13): Sider 顶级 4 组 + 「产品」组 3 项（产品分类已删）+ 顺序. */
  it('renders the 产品 menu group with 3 items between 组织 and 需求管理 (TC-FES-PROD-001)', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/" element={<div>home</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );
    // Top-level groups present (组织 / 产品 / 需求管理 / 人事配置 / 系统 since v0.0.15).
    expect(screen.getByText('组织')).toBeInTheDocument();
    // "产品" matches both the group title and the 产品 item — getAllByText finds both.
    expect(screen.getAllByText('产品').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('需求管理')).toBeInTheDocument();
    expect(screen.getByText('人事配置')).toBeInTheDocument();
    // 产品 group has 3 items — 产品分类 removed in v0.0.13.
    expect(screen.queryByText('产品分类')).toBeNull();
    expect(screen.queryByTestId('appshell-nav-/pm/product-categories')).toBeNull();
    expect(screen.getByText('产品模块')).toBeInTheDocument();
    expect(screen.getByText('功能')).toBeInTheDocument();
    // Use testid to disambiguate 「产品」 item from 「产品」 group title.
    expect(screen.getByTestId('appshell-nav-/pm/products')).toHaveAttribute(
      'href',
      '/pm/products',
    );
    expect(screen.getByTestId('appshell-nav-/pm/features')).toHaveAttribute(
      'href',
      '/pm/features',
    );
    // Order: 产品组 (/pm/products) 位于 需求管理组 (/pm/projects) 之前.
    const sider = screen.getByTestId('appshell-sider');
    const html = sider.innerHTML;
    expect(html.indexOf('/pm/products')).toBeLessThan(html.indexOf('/pm/projects'));
    // 产品组 3 项内部顺序: 产品 → 产品模块 → 功能.
    expect(html.indexOf('/pm/products')).toBeLessThan(html.indexOf('/pm/product-modules'));
    expect(html.indexOf('/pm/product-modules')).toBeLessThan(html.indexOf('/pm/features'));
  });

  /** TC-FES-H01: Sider 含「人事配置」菜单组 + 3 子项 + /hr/positions 链接。 */
  it('renders the 人事配置 menu group with 3 items (TC-FES-H01)', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/" element={<div>home</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );
    expect(screen.getByText('人事配置')).toBeInTheDocument();
    expect(screen.getByText('岗位')).toBeInTheDocument();
    expect(screen.getByText('角色')).toBeInTheDocument();
    expect(screen.getByText('用户角色')).toBeInTheDocument();
    expect(screen.getByTestId('appshell-nav-/hr/positions')).toHaveAttribute(
      'href',
      '/hr/positions',
    );
  });

  /** TC-FES-AUD-001 (v0.0.15): Sider 第 5 顶级组「系统」末位 + 审计日志 → /sys/audit-logs. */
  it('renders the 系统 menu group last with 审计日志 (TC-FES-AUD-001)', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/" element={<div>home</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );
    // 5 top-level groups; 系统 present and last.
    expect(screen.getByText('系统')).toBeInTheDocument();
    expect(screen.getByText('审计日志')).toBeInTheDocument();
    expect(screen.getByTestId('appshell-nav-/sys/audit-logs')).toHaveAttribute(
      'href',
      '/sys/audit-logs',
    );
    const html = screen.getByTestId('appshell-sider').innerHTML;
    // 系统组 (/sys/audit-logs) 位于 人事配置组 (/hr/positions) 之后（末位）.
    expect(html.indexOf('/hr/positions')).toBeLessThan(html.indexOf('/sys/audit-logs'));
  });

  const renderShell = () =>
    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<AppLayout />}>
            <Route path="/" element={<div>home</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

  /** v0.0.18 TC-FES-WB-101: 工作台 group fronts the nav with 我的工作台 → /. */
  it('renders the 工作台 group first with 我的工作台 → / (TC-FES-WB-101)', () => {
    renderShell();
    expect(screen.getByText('工作台')).toBeInTheDocument();
    expect(screen.getByTestId('appshell-nav-/')).toHaveAttribute('href', '/');
    // 工作台 (我的工作台) appears before 组织 (组织节点).
    const html = screen.getByTestId('appshell-sider').innerHTML;
    expect(html.indexOf('我的工作台')).toBeLessThan(html.indexOf('组织节点'));
  });

  /** v0.0.18 TC-FES-WB-102: brand links to the workbench. */
  it('brand links to the workbench (TC-FES-WB-102)', () => {
    renderShell();
    expect(screen.getByTestId('appshell-brand')).toHaveAttribute('href', '/');
  });

  /** v0.0.18 TC-FES-WB-103: a group folds/unfolds when its title is clicked. */
  it('collapses and expands a menu group on title click (TC-FES-WB-103)', () => {
    renderShell();
    expect(screen.getByText('审计日志')).toBeInTheDocument();
    fireEvent.click(screen.getByTestId('appshell-group-sys'));
    expect(screen.queryByText('审计日志')).toBeNull();
    fireEvent.click(screen.getByTestId('appshell-group-sys'));
    expect(screen.getByText('审计日志')).toBeInTheDocument();
  });

  /** v0.0.18 TC-FES-WB-104: the whole Sider hides when the toggle is clicked. */
  it('hides the Sider when the toggle is clicked (TC-FES-WB-104)', () => {
    renderShell();
    expect(screen.getByTestId('appshell-sider')).toBeInTheDocument();
    fireEvent.click(screen.getByTestId('appshell-sider-toggle'));
    expect(screen.queryByTestId('appshell-sider')).toBeNull();
    fireEvent.click(screen.getByTestId('appshell-sider-toggle'));
    expect(screen.getByTestId('appshell-sider')).toBeInTheDocument();
  });

  /** v0.0.20 TC-FES-RN-001: a non-admin user sees only 工作台 + 需求管理. */
  it('shows only 工作台 and 需求管理 to a non-admin user (TC-FES-RN-001)', () => {
    useAuthStore.setState({
      token: 'tk',
      user: { username: 'bob', roles: [role(false)] },
    });
    renderShell();
    // All-users groups present.
    expect(screen.getByText('工作台')).toBeInTheDocument();
    expect(screen.getByText('需求管理')).toBeInTheDocument();
    // Admin-only groups hidden.
    expect(screen.queryByText('组织')).toBeNull();
    expect(screen.queryByText('人事配置')).toBeNull();
    expect(screen.queryByText('系统')).toBeNull();
    expect(screen.queryByTestId('appshell-nav-/org/organizations')).toBeNull();
    expect(screen.queryByTestId('appshell-nav-/sys/audit-logs')).toBeNull();
    // 产品 group title hidden — its item links must be gone too.
    expect(screen.queryByTestId('appshell-nav-/pm/products')).toBeNull();
  });

  /** v0.0.20 TC-FES-RN-002: an admin user sees all six groups. */
  it('shows all six groups to an admin user (TC-FES-RN-002)', () => {
    useAuthStore.setState({
      token: 'tk',
      user: { username: 'alice', roles: [role(false), role(true)] },
    });
    renderShell();
    expect(screen.getByText('工作台')).toBeInTheDocument();
    expect(screen.getByText('组织')).toBeInTheDocument();
    expect(screen.getAllByText('产品').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText('需求管理')).toBeInTheDocument();
    expect(screen.getByText('人事配置')).toBeInTheDocument();
    expect(screen.getByText('系统')).toBeInTheDocument();
    expect(screen.getByTestId('appshell-nav-/sys/audit-logs')).toBeInTheDocument();
  });
});
