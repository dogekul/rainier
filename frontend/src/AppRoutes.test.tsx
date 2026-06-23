import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AppRoutes } from './AppRoutes';
import { isAdminPath } from './components/ProtectedRoute';
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
vi.mock('./api/reviews', async () => {
  const actual = await vi.importActual<typeof import('./api/reviews')>('./api/reviews');
  return {
    ...actual,
    getPendingReviews: vi.fn().mockResolvedValue([]),
  };
});
vi.mock('./api/profile', async () => {
  const actual = await vi.importActual<typeof import('./api/profile')>('./api/profile');
  return {
    ...actual,
    getMyProfile: vi.fn().mockResolvedValue({
      userId: 1,
      loginName: 'alice',
      name: 'Alice',
      positionName: null,
      positionCategory: null,
      memberships: [],
      manager: null,
      ownedStoryCount: 0,
      assignedTaskCount: 0,
    }),
  };
});
vi.mock('./api/compliance', async () => {
  const actual = await vi.importActual<typeof import('./api/compliance')>('./api/compliance');
  return {
    ...actual,
    getAuditSummary: vi
      .fn()
      .mockResolvedValue({ total: 0, byAction: [], byEntityType: [], recent: [] }),
    getResidualPermissions: vi.fn().mockResolvedValue([]),
  };
});
vi.mock('./api/inbox', async () => {
  const actual = await vi.importActual<typeof import('./api/inbox')>('./api/inbox');
  return {
    ...actual,
    getInbox: vi.fn().mockResolvedValue({ unconvertedDemands: [], myRequirements: [] }),
  };
});
vi.mock('./api/aiWorkLog', async () => {
  const actual = await vi.importActual<typeof import('./api/aiWorkLog')>('./api/aiWorkLog');
  return {
    ...actual,
    listAiWorkLogs: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 }),
  };
});
vi.mock('./api/opportunity', async () => {
  const actual = await vi.importActual<typeof import('./api/opportunity')>('./api/opportunity');
  return {
    ...actual,
    listOpportunities: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 200 }),
  };
});
vi.mock('./api/operation', async () => {
  const actual = await vi.importActual<typeof import('./api/operation')>('./api/operation');
  return {
    ...actual,
    listOperations: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 200 }),
  };
});
vi.mock('./api/customer', async () => {
  const actual = await vi.importActual<typeof import('./api/customer')>('./api/customer');
  return {
    ...actual,
    listCustomers: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 20 }),
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

  /** TC-FES-COMP-01/02 (v0.0.41): /sys/compliance route registered + mounts CompliancePage. */
  it('registers /sys/compliance → CompliancePage (TC-FES-COMP-01/02)', async () => {
    const path = resolve(__dirname, 'AppRoutes.tsx');
    const src = readFileSync(path, 'utf-8');
    expect(src.split('/sys/compliance').length - 1).toBeGreaterThanOrEqual(1);
    render(
      <MemoryRouter initialEntries={['/sys/compliance']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('compliance-summary')).toBeInTheDocument();
    });
  });

  /** TC-FES-CRM-01 (v0.0.44/45): all five /crm/* routes registered + 商机看板 mounts (all-users). */
  it('registers the five /crm/* routes (TC-FES-CRM-01)', async () => {
    const path = resolve(__dirname, 'AppRoutes.tsx');
    const src = readFileSync(path, 'utf-8');
    expect(src.split('/crm/customers').length - 1).toBeGreaterThanOrEqual(1);
    expect(src.split('/crm/opportunities').length - 1).toBeGreaterThanOrEqual(1);
    expect(src.split('/crm/presale-flow').length - 1).toBeGreaterThanOrEqual(1);
    expect(src.split('/crm/delivery-flow').length - 1).toBeGreaterThanOrEqual(1);
    expect(src.split('/crm/operations').length - 1).toBeGreaterThanOrEqual(1);
    render(
      <MemoryRouter initialEntries={['/crm/opportunities']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByTestId('opp-summary')).toBeInTheDocument());
  });

  /** TC-FES-CRM-05 (v0.0.45): 客户 page mounts at /crm/customers. */
  it('mounts CustomerPage at /crm/customers (TC-FES-CRM-05)', async () => {
    render(
      <MemoryRouter initialEntries={['/crm/customers']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByTestId('customers-new-btn')).toBeInTheDocument());
  });

  it('mounts OperationBoard at /crm/operations', async () => {
    render(
      <MemoryRouter initialEntries={['/crm/operations']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByTestId('opr-summary')).toBeInTheDocument());
  });

  /** TC-FES-CRM-02 (v0.0.44): 售前流转 operation page mounts at /crm/presale-flow. */
  it('mounts PresaleFlow at /crm/presale-flow (TC-FES-CRM-02)', async () => {
    render(
      <MemoryRouter initialEntries={['/crm/presale-flow']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByTestId('presale-summary')).toBeInTheDocument());
  });

  /** TC-FES-CRM-03 (v0.0.44): 实施流转 operation page mounts at /crm/delivery-flow. */
  it('mounts DeliveryFlow at /crm/delivery-flow (TC-FES-CRM-03)', async () => {
    render(
      <MemoryRouter initialEntries={['/crm/delivery-flow']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByTestId('delivery-summary')).toBeInTheDocument());
  });

  /** TC-FES-CRM-04 (v0.0.44): all four /crm/* routes are all-users (NOT admin-gated). */
  it('keeps all /crm/* routes all-users — isAdminPath false (TC-FES-CRM-04)', () => {
    for (const p of [
      '/crm/customers',
      '/crm/opportunities',
      '/crm/presale-flow',
      '/crm/delivery-flow',
      '/crm/operations',
    ]) {
      expect(isAdminPath(p)).toBe(false);
    }
  });

  /** TC-FES-AIW-01/02 (v0.0.43): /ai/work-logs route registered + mounts AiWorkLogsPage (all-users). */
  it('registers /ai/work-logs → AiWorkLogsPage (TC-FES-AIW-01/02)', async () => {
    const path = resolve(__dirname, 'AppRoutes.tsx');
    const src = readFileSync(path, 'utf-8');
    expect(src.split('/ai/work-logs').length - 1).toBeGreaterThanOrEqual(1);
    render(
      <MemoryRouter initialEntries={['/ai/work-logs']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('ai-summary')).toBeInTheDocument();
    });
  });

  /** TC-FES-INBOX-01/02 (v0.0.42): /inbox route registered + mounts InboxPage (all-users). */
  it('registers /inbox → InboxPage (TC-FES-INBOX-01/02)', async () => {
    const path = resolve(__dirname, 'AppRoutes.tsx');
    const src = readFileSync(path, 'utf-8');
    expect(src.split('/inbox').length - 1).toBeGreaterThanOrEqual(1);
    render(
      <MemoryRouter initialEntries={['/inbox']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('inbox-summary')).toBeInTheDocument();
    });
  });

  /** TC-FES-PROF-01/02 (v0.0.40): /profile route registered + mounts ProfilePage (all-users). */
  it('registers /profile → ProfilePage (TC-FES-PROF-01/02)', async () => {
    const path = resolve(__dirname, 'AppRoutes.tsx');
    const src = readFileSync(path, 'utf-8');
    expect(src.split('/profile').length - 1).toBeGreaterThanOrEqual(1);
    render(
      <MemoryRouter initialEntries={['/profile']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('profile-identity')).toBeInTheDocument();
    });
  });

  /** TC-FES-REV-01/02 (v0.0.39): /reviews route registered + mounts ReviewsPage (all-users). */
  it('registers /reviews → ReviewsPage (TC-FES-REV-01/02)', async () => {
    const path = resolve(__dirname, 'AppRoutes.tsx');
    const src = readFileSync(path, 'utf-8');
    expect(src.split('/reviews').length - 1).toBeGreaterThanOrEqual(1);
    render(
      <MemoryRouter initialEntries={['/reviews']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('reviews-empty')).toBeInTheDocument();
    });
  });

  /** TC-FES-AUD-004 (v0.0.15): /sys/audit-logs route registered + mounts AuditLogsPage. */
  it('registers /sys/audit-logs → AuditLogsPage (TC-FES-AUD-004)', async () => {
    const path = resolve(__dirname, 'AppRoutes.tsx');
    const src = readFileSync(path, 'utf-8');
    expect(src.split('/sys/audit-logs').length - 1).toBeGreaterThanOrEqual(1);
    render(
      <MemoryRouter initialEntries={['/sys/audit-logs']}>
        <AppRoutes />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('audit-query-btn')).toBeInTheDocument();
    });
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
