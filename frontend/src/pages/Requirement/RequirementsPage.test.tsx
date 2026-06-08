import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
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

vi.mock('../../api/sprint', async () => {
  const actual = await vi.importActual<typeof import('../../api/sprint')>('../../api/sprint');
  return {
    ...actual,
    listSprints: vi.fn().mockResolvedValue({
      content: [
        {
          id: 10,
          code: 'SPR-A',
          name: 'Phase 1',
          status: 'PLANNING',
          requirementId: 1,
          ownerUserId: 1,
          storyCount: 0,
        },
        {
          id: 11,
          code: 'SPR-B',
          name: 'Phase 2',
          status: 'ACTIVE',
          requirementId: 1,
          ownerUserId: 1,
          storyCount: 0,
        },
      ],
      total: 2,
      page: 0,
      size: 100,
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

describe('RequirementsPage drilldown', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
  });

  /** TC-FES-SPR-05 (v0.0.10): 表格含 "Sprint 数" 列 + 单元格显示 r.sprintCount. */
  it('renders Sprint 数 column with sprintCount value (TC-FES-SPR-05)', async () => {
    render(<RequirementsPage />);
    await waitFor(() => {
      expect(screen.getByText('REQ-1')).toBeInTheDocument();
    });
    expect(screen.getByText('Sprint 数')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByTestId('req-expand-btn-1')).toBeInTheDocument();
  });

  /** TC-FES-SPR-05 part B (v0.0.10): expanding a row renders SprintListPanel + sub-table. */
  it('expanding a row renders the SprintListPanel sub-table (TC-FES-SPR-05)', async () => {
    render(<RequirementsPage />);
    await waitFor(() => {
      expect(screen.getByTestId('req-expand-btn-1')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId('req-expand-btn-1'));
    await waitFor(() => {
      expect(screen.getByTestId('sprint-list-panel-1')).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText('SPR-A')).toBeInTheDocument();
      expect(screen.getByText('SPR-B')).toBeInTheDocument();
    });
    expect(screen.getByTestId('sprints-new-btn')).toBeInTheDocument();
  });
});
