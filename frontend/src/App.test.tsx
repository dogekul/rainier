import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it } from 'vitest';
import { AppRoutes } from './AppRoutes';
import './styles/global.css';
import { useAuthStore } from './store/auth';

describe('App smoke', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: null, user: null });
    window.localStorage.removeItem('rainier.token');
  });

  it('renders without crashing under MemoryRouter', () => {
    render(
      <MemoryRouter initialEntries={['/login']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    expect(screen.getByText('Rainier 登录')).toBeInTheDocument();
  });
});
