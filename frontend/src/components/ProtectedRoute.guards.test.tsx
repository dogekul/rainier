import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ProtectedRoute } from './ProtectedRoute';
import { me } from '../api/auth';
import { useAuthStore } from '../store/auth';

// me() is the app-level hydration call ProtectedRoute makes on entry — stub it per test.
vi.mock('../api/auth', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../api/auth')>();
  return { ...actual, me: vi.fn() };
});

const meMock = vi.mocked(me);

function meResult(adminAccess: boolean) {
  return {
    id: null,
    username: 'u',
    name: 'U',
    roles: [
      {
        roleId: 1,
        roleCode: 'R',
        roleName: 'R',
        projectId: null,
        projectName: null,
        projectCode: null,
        adminAccess,
      },
    ],
    projects: [],
  };
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route element={<ProtectedRoute />}>
          <Route path="/" element={<div data-testid="wb-stub">WB</div>} />
          <Route path="/hr/roles" element={<div data-testid="roles-stub">ROLES</div>} />
          <Route path="/pm/projects" element={<div data-testid="projects-stub">PROJ</div>} />
          <Route path="/pm/products" element={<div data-testid="products-stub">PRODUCTS</div>} />
        </Route>
      </Routes>
    </MemoryRouter>,
  );
}

describe('ProtectedRoute admin guards (v0.0.20)', () => {
  beforeEach(() => {
    meMock.mockReset();
    // token present, user has NO role context yet → ProtectedRoute hydrates via me().
    useAuthStore.setState({ token: 'tk', user: { username: 'u' } });
  });

  /** TC-FES-RN-003: non-admin hitting an admin route is redirected home after hydration. */
  it('redirects a non-admin from an admin route to / (TC-FES-RN-003)', async () => {
    meMock.mockResolvedValue(meResult(false));
    renderAt('/hr/roles');
    await waitFor(() => expect(screen.getByTestId('wb-stub')).toBeInTheDocument());
    expect(screen.queryByTestId('roles-stub')).toBeNull();
  });

  /**
   * TC-FES-RN-003b (review PA-1): the 产品 admin group lives under `/pm/...` next to the all-users
   * 需求管理 group — prove a non-admin IS bounced from `/pm/products` (the exact prefix-collision case).
   */
  it('redirects a non-admin from a /pm/products admin route to / (TC-FES-RN-003b)', async () => {
    meMock.mockResolvedValue(meResult(false));
    renderAt('/pm/products');
    await waitFor(() => expect(screen.getByTestId('wb-stub')).toBeInTheDocument());
    expect(screen.queryByTestId('products-stub')).toBeNull();
  });

  /** TC-FES-RN-004: an admin stays on the admin route (falsifiable: a wrong redirect drops roles-stub). */
  it('keeps an admin on an admin route (TC-FES-RN-004)', async () => {
    meMock.mockResolvedValue(meResult(true));
    renderAt('/hr/roles');
    await waitFor(() => expect(meMock).toHaveBeenCalled());
    await waitFor(() => expect(useAuthStore.getState().user?.roles).toBeDefined());
    expect(screen.getByTestId('roles-stub')).toBeInTheDocument();
  });

  /** TC-FES-RN-005: a 需求管理 (/pm) route stays open to non-admins. */
  it('keeps a non-admin on a /pm 需求管理 route (TC-FES-RN-005)', async () => {
    meMock.mockResolvedValue(meResult(false));
    renderAt('/pm/projects');
    await waitFor(() => expect(meMock).toHaveBeenCalled());
    await waitFor(() => expect(useAuthStore.getState().user?.roles).toBeDefined());
    expect(screen.getByTestId('projects-stub')).toBeInTheDocument();
  });

  /** TC-FES-RN-006: ProtectedRoute hydrates the current user into the store exactly once. */
  it('hydrates the current user into the store once on entry (TC-FES-RN-006)', async () => {
    meMock.mockResolvedValue(meResult(true));
    renderAt('/');
    await waitFor(() => expect(meMock).toHaveBeenCalledTimes(1));
    await waitFor(() => expect(useAuthStore.getState().user?.roles).toBeDefined());
    expect(useAuthStore.getState().user?.roles?.[0].adminAccess).toBe(true);
  });
});
