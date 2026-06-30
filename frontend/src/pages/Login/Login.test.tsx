import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { login, me } from '../../api/auth';
import { useAuthStore } from '../../store/auth';
import Login from './index';

const navigate = vi.fn();

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return { ...actual, useNavigate: () => navigate };
});

vi.mock('../../api/auth', async (orig) => ({
  ...(await orig<typeof import('../../api/auth')>()),
  login: vi.fn(),
  me: vi.fn(),
}));

/**
 * Covers TC-FES-004: the primary submit button uses the feishu primary color
 * (rgb(51, 112, 255)) via the {@code --rainier-color-primary} token. Reads computed style,
 * agnostic to className.
 */
describe('Login page', () => {
  beforeEach(() => {
    navigate.mockClear();
    vi.mocked(login).mockReset();
    vi.mocked(me).mockReset();
    useAuthStore.setState({ token: null, user: null });
    window.localStorage.removeItem('rainier.token');
  });

  it('submit button uses --rainier-color-primary as background-color', () => {
    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>,
    );
    const button = screen.getByTestId('login-submit') as HTMLButtonElement;
    const style = window.getComputedStyle(button);
    // jsdom does NOT resolve var() inside getComputedStyle (known limitation), so we assert that
    // the button reaches for the token rather than hardcoding a color. The token → rgb(51,112,255)
    // mapping is covered by TC-FES-003 (tokens.test.tsx). End-to-end visual check is covered by
    // TC-DRT-001 browser smoke. Documented in pending-adjustments.md.
    expect(style.backgroundColor).toBe('var(--rainier-color-primary)');
  });

  it('navigates to defaultLandingPath after login hydration (H6-S7)', async () => {
    vi.mocked(login).mockResolvedValue({ token: 'jwt-1', user: { username: 'pmo' } });
    vi.mocked(me).mockResolvedValue({
      id: 9,
      username: 'pmo',
      name: 'PMO',
      roles: [],
      projects: [],
      defaultLandingPath: '/pmo',
    });

    render(
      <MemoryRouter>
        <Login />
      </MemoryRouter>,
    );

    fireEvent.change(screen.getByTestId('login-username'), { target: { value: 'pmo' } });
    fireEvent.change(screen.getByTestId('login-password'), { target: { value: 'rainier123' } });
    fireEvent.click(screen.getByTestId('login-submit'));

    await waitFor(() => expect(navigate).toHaveBeenCalledWith('/pmo', { replace: true }));
    expect(useAuthStore.getState().user?.defaultLandingPath).toBe('/pmo');
  });
});
