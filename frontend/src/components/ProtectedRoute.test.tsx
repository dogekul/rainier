import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import { AppRoutes } from '../AppRoutes';
import { useAuthStore } from '../store/auth';

/**
 * Covers TC-FES-001 (unauthenticated → /login) and TC-FES-002 (authenticated → Home with username
 * shown in header and main greeting).
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
    expect(screen.queryByTestId('home-greeting')).not.toBeInTheDocument();
  });

  it('renders Home with username when authenticated (TC-FES-002)', () => {
    useAuthStore.setState({ token: 'fake-token', user: { username: 'alice' } });

    render(
      <MemoryRouter initialEntries={['/']}>
        <AppRoutes />
      </MemoryRouter>,
    );

    expect(screen.getByTestId('appshell-username')).toHaveTextContent('alice');
    expect(screen.getByTestId('home-greeting')).toHaveTextContent('Hello, alice');
  });
});
