import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { OrganizationDetailPage } from './OrganizationDetailPage';
import { getOrganization, type Organization } from '../../api/organization';

vi.mock('../../store/auth', async (orig) => {
  const actual = await orig<typeof import('../../store/auth')>();
  return {
    ...actual,
    useAuthStore: (
      selector: (s: { user: { id: number; username: string; roles: { adminAccess: boolean }[] } }) => unknown,
    ) => selector({ user: { id: 1, username: 'alice', roles: [{ adminAccess: true }] } }),
  };
});

vi.mock('../../api/organization', async (orig) => {
  const actual = await orig<typeof import('../../api/organization')>();
  return {
    ...actual,
    getOrganization: vi.fn(),
    listOrganizations: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 200 }),
    listOrganizationAuditLog: vi
      .fn()
      .mockResolvedValue({ content: [], total: 0, page: 0, size: 50 }),
  };
});
vi.mock('../../api/organizationPmo', () => ({
  listOrganizationPmos: vi.fn().mockResolvedValue([]),
}));
vi.mock('../../api/userOrganization', () => ({
  listUserOrganizations: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 200 }),
}));
vi.mock('../../api/project', () => ({
  listProjects: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 200 }),
}));

function renderAt(id: number) {
  return render(
    <MemoryRouter initialEntries={[`/org/orgs/${id}`]}>
      <Routes>
        <Route path="/org/orgs/:id" element={<OrganizationDetailPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('OrganizationDetailPage (E3)', () => {
  beforeEach(() => {
    vi.mocked(getOrganization).mockReset().mockResolvedValue({
      id: 1,
      parentId: null,
      type: 'COMPANY',
      code: 'HQ',
      name: '总公司',
      wholeName: '总公司',
      enabled: true,
    } as Organization);
  });

  it('renders org name + code and all tabs', async () => {
    renderAt(1);
    await waitFor(() => expect(screen.getByTestId('org-detail')).toBeInTheDocument());
    expect(screen.getAllByText('总公司').length).toBeGreaterThan(0);
    expect(screen.getAllByText('HQ').length).toBeGreaterThan(0);
    const tabs = screen.getByTestId('org-detail-tabs');
    // 6 tabs: 基本信息 / 成员 / PMO / 子组织 / 关联项目 / 变更历史
    expect(tabs.querySelectorAll('button.pm-tab').length).toBeGreaterThanOrEqual(6);
    expect(screen.getByTestId('org-tab-audit')).toBeInTheDocument();
  });
});
