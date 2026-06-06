import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import { useAuthStore } from '../store/auth';
import { AppLayout } from './AppLayout';

describe('AppLayout Sider (TC-FES-201)', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
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
});
