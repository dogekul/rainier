import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { RequirementsPage } from './RequirementsPage';
import { useAuthStore } from '../../store/auth';

vi.mock('../../api/requirement', async () => {
  const actual = await vi.importActual<typeof import('../../api/requirement')>(
    '../../api/requirement',
  );
  return {
    ...actual,
    listRequirements: vi.fn().mockResolvedValue({
      content: [
        {
          id: 1,
          code: 'REQ-1',
          title: '登录流程',
          ownerUserId: 1,
          ownerName: 'Alice',
          ownerLoginName: 'alice',
          status: 'DRAFT',
          priority: 'MEDIUM',
          sprintCount: 3,
        },
      ],
      total: 1,
      page: 0,
      size: 20,
    }),
  };
});

vi.mock('../../api/user', async () => {
  const actual = await vi.importActual<typeof import('../../api/user')>('../../api/user');
  return {
    ...actual,
    listUsers: vi.fn().mockResolvedValue({
      content: [
        { id: 1, loginName: 'alice', name: 'Alice', isInternal: true, enabled: true },
      ],
      total: 1,
      page: 0,
      size: 100,
    }),
  };
});

describe('RequirementsPage', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
  });

  /** TC-FES-SPR-05 (v0.0.10 → v0.0.61): 表格含 "Sprint 数" 列 + 单元格显示 r.sprintCount.
   *  v0.0.61: 「展开」按钮废弃，验证存在 req-row-{id} 即可。 */
  it('renders Sprint 数 column + clickable row (TC-FES-SPR-05)', async () => {
    render(
      <MemoryRouter>
        <RequirementsPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByText('REQ-1')).toBeInTheDocument();
    });
    expect(screen.getByText('Sprint 数')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByTestId('req-row-1')).toBeInTheDocument();
  });

  /** TC-FES-REQ-DETAIL-01 (v0.0.61): row click 跳转 /pm/requirements/:id，替代了 v0.0.10 的「展开」逻辑。 */
  it('row click navigates to /pm/requirements/:id', async () => {
    render(
      <MemoryRouter initialEntries={['/pm/requirements']}>
        <Routes>
          <Route path="/pm/requirements" element={<RequirementsPage />} />
          <Route path="/pm/requirements/:id" element={<div data-testid="req-detail-stub">detail</div>} />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('req-row-1')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId('req-row-1'));
    await waitFor(() => {
      expect(screen.getByTestId('req-detail-stub')).toBeInTheDocument();
    });
  });
});
