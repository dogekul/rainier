import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
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
vi.mock('./api/project', async () => {
  const actual = await vi.importActual<typeof import('./api/project')>('./api/project');
  return {
    ...actual,
    listProjects: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 20 }),
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

  /** TC-FES-P02 (v0.0.8): /pm redirects to /pm/projects (was /pm/demands). */
  it('redirects /pm to /pm/projects (TC-FES-P02 redirect)', async () => {
    render(
      <MemoryRouter initialEntries={['/pm']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('projects-new-btn')).toBeInTheDocument();
    });
  });

  /**
   * TC-FES-P02 belt-and-suspenders: literal route string must appear at least once in
   * AppRoutes.tsx — guards against a linter "unused import" sweep silently dropping the route.
   */
  it('AppRoutes.tsx contains /pm/projects route literal (TC-FES-P02 guard)', () => {
    const path = resolve(__dirname, 'AppRoutes.tsx');
    const src = readFileSync(path, 'utf-8');
    const occurrences = src.split('/pm/projects').length - 1;
    expect(occurrences).toBeGreaterThanOrEqual(1);
  });

  /**
   * TC-FES-PROD-002 (v0.0.13 沿用): /pm/products route literal still registered.
   * TC-FES-PROD-003 (v0.0.13): /pm/product-categories route fully removed — grep = 0.
   */
  it('keeps /pm/products and drops /pm/product-categories route literals (TC-FES-PROD-002/003)', () => {
    const path = resolve(__dirname, 'AppRoutes.tsx');
    const src = readFileSync(path, 'utf-8');
    expect(src.split('/pm/products').length - 1).toBeGreaterThanOrEqual(1);
    expect(src.split('/pm/product-categories').length - 1).toBe(0);
    expect(src.includes('ProductCategoriesPage')).toBe(false);
  });

  /** TC-FES-P02 (v0.0.8): /pm/projects mounts ProjectsPage. */
  it('mounts ProjectsPage at /pm/projects (TC-FES-P02)', async () => {
    render(
      <MemoryRouter initialEntries={['/pm/projects']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('projects-new-btn')).toBeInTheDocument();
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
