import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { me } from '../api/auth';
import { AppRoutes } from '../AppRoutes';
import { useAuthStore } from '../store/auth';
import { ProtectedRoute } from './ProtectedRoute';

// The protected landing page (WorkbenchPage) fetches GET /api/auth/me on mount — stub it so this
// routing/protection test doesn't hit the network. id:null keeps it from loading tasks/stories.
vi.mock('../api/auth', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/auth')>();
  return {
    ...actual,
    me: vi
      .fn()
      .mockResolvedValue({ id: null, username: 'alice', name: 'Alice', roles: [], projects: [] }),
  };
});

/**
 * Covers TC-FES-001 (unauthenticated → /login) and TC-FES-002 (authenticated → protected workbench
 * with username in the header).
 */
describe('ProtectedRoute via full route tree', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: null, user: null });
    window.localStorage.removeItem('rainier.token');
  });

  it('redirects to /login when there is no token (TC-FES-001)', () => {
    render(
      <MemoryRouter initialEntries={['/']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    expect(screen.getByText('Rainier 登录')).toBeInTheDocument();
    expect(screen.queryByTestId('workbench-greeting')).not.toBeInTheDocument();
  });

  it('renders the protected workbench when authenticated (TC-FES-002)', async () => {
    useAuthStore.setState({ token: 'fake-token', user: { username: 'alice' } });

    render(
      <MemoryRouter initialEntries={['/']}>
        <AppRoutes />
      </MemoryRouter>,
    );

    expect(screen.getByTestId('appshell-username')).toHaveTextContent('alice');
    expect(screen.getByTestId('workbench-greeting')).toBeInTheDocument();
    // Await the me() resolution so the post-mount state update is wrapped (silences the act warning).
    await waitFor(() =>
      expect(screen.getByTestId('workbench-greeting')).toHaveTextContent('Alice'),
    );
  });

  it('redirects / to defaultLandingPath after hydration (H6-S8)', async () => {
    useAuthStore.setState({ token: 'fake-token', user: { username: 'pmo' } });
    vi.mocked(me).mockResolvedValueOnce({
      id: 2,
      username: 'pmo',
      name: 'PMO',
      roles: [],
      projects: [],
      defaultLandingPath: '/pmo',
    });

    render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route element={<ProtectedRoute />}>
            <Route path="/" element={<div data-testid="home-page" />} />
            <Route path="/pmo" element={<div data-testid="pmo-page" />} />
          </Route>
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => expect(screen.getByTestId('pmo-page')).toBeInTheDocument());
    expect(screen.queryByTestId('home-page')).not.toBeInTheDocument();
  });
});
