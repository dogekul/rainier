import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AppRoutes } from './AppRoutes';
import { useAuthStore } from './store/auth';

// Mock the API modules so the pages mount without network.
vi.mock('./api/demand', async () => {
  const actual = await vi.importActual<typeof import('./api/demand')>('./api/demand');
  return {
    ...actual,
    listDemands: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 20 }),
  };
});
vi.mock('./api/requirement', async () => {
  const actual = await vi.importActual<typeof import('./api/requirement')>('./api/requirement');
  return {
    ...actual,
    listRequirements: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 20 }),
  };
});
vi.mock('./api/demandRequirement', async () => {
  const actual = await vi.importActual<typeof import('./api/demandRequirement')>(
    './api/demandRequirement',
  );
  return {
    ...actual,
    listDemandRequirements: vi
      .fn()
      .mockResolvedValue({ content: [], total: 0, page: 0, size: 20 }),
  };
});
vi.mock('./api/position', async () => {
  const actual = await vi.importActual<typeof import('./api/position')>('./api/position');
  return {
    ...actual,
    listPositions: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 20 }),
  };
});
vi.mock('./api/role', async () => {
  const actual = await vi.importActual<typeof import('./api/role')>('./api/role');
  return {
    ...actual,
    listRoles: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 20 }),
  };
});
vi.mock('./api/userRole', async () => {
  const actual = await vi.importActual<typeof import('./api/userRole')>('./api/userRole');
  return {
    ...actual,
    listUserRoles: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 20 }),
  };
});

describe('AppRoutes /pm/*', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
  });

  /** TC-FES-D02: /pm/demands route resolves to a page that has the new-demand button. */
  it('mounts DemandsPage at /pm/demands (TC-FES-D02)', async () => {
    render(
      <MemoryRouter initialEntries={['/pm/demands']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('demands-new-btn')).toBeInTheDocument();
    });
  });

  it('redirects /pm to /pm/demands', async () => {
    render(
      <MemoryRouter initialEntries={['/pm']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('demands-new-btn')).toBeInTheDocument();
    });
  });

  it('mounts RequirementsPage at /pm/requirements', async () => {
    render(
      <MemoryRouter initialEntries={['/pm/requirements']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('req-new-btn')).toBeInTheDocument();
    });
  });

  it('mounts LinksPage at /pm/demand-requirements', async () => {
    render(
      <MemoryRouter initialEntries={['/pm/demand-requirements']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('links-new-btn')).toBeInTheDocument();
    });
  });

  /** TC-FES-H02: /hr/positions route resolves to PositionsPage. */
  it('mounts PositionsPage at /hr/positions (TC-FES-H02)', async () => {
    render(
      <MemoryRouter initialEntries={['/hr/positions']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('positions-new-btn')).toBeInTheDocument();
    });
  });

  it('redirects /hr to /hr/positions', async () => {
    render(
      <MemoryRouter initialEntries={['/hr']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('positions-new-btn')).toBeInTheDocument();
    });
  });

  it('mounts RolesPage at /hr/roles', async () => {
    render(
      <MemoryRouter initialEntries={['/hr/roles']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('roles-new-btn')).toBeInTheDocument();
    });
  });

  it('mounts UserRolesPage at /hr/user-roles', async () => {
    render(
      <MemoryRouter initialEntries={['/hr/user-roles']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('user-roles-new-btn')).toBeInTheDocument();
    });
  });
});
