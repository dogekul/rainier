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
});
