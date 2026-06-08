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
          storyCount: 3,
        },
      ],
      total: 1,
      page: 0,
      size: 20,
    }),
  };
});

vi.mock('../../api/story', async () => {
  const actual = await vi.importActual<typeof import('../../api/story')>('../../api/story');
  return {
    ...actual,
    listStories: vi.fn().mockResolvedValue({
      content: [
        {
          id: 10,
          code: 'STR-10',
          title: 'S10',
          status: 'DRAFT',
          priority: 'MEDIUM',
          requirementId: 1,
          ownerUserId: 1,
        },
        {
          id: 11,
          code: 'STR-11',
          title: 'S11',
          status: 'IN_PROGRESS',
          priority: 'HIGH',
          requirementId: 1,
          ownerUserId: 1,
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

  /** TC-FES-S01: 表格含 "Story 数" 列 + 单元格显示 r.storyCount. */
  it('renders Story 数 column with storyCount value (TC-FES-S01)', async () => {
    render(<RequirementsPage />);
    await waitFor(() => {
      expect(screen.getByText('REQ-1')).toBeInTheDocument();
    });
    expect(screen.getByText('Story 数')).toBeInTheDocument();
    // The storyCount cell renders "3" for the seeded Requirement.
    expect(screen.getByText('3')).toBeInTheDocument();
    // Expand button is present.
    expect(screen.getByTestId('req-expand-btn-1')).toBeInTheDocument();
  });

  /** TC-FES-S02: 点开行渲染 StoryListPanel + 子表含 STR-10/STR-11 + 新建按钮. */
  it('expanding a row renders the StoryListPanel sub-table (TC-FES-S02)', async () => {
    render(<RequirementsPage />);
    await waitFor(() => {
      expect(screen.getByTestId('req-expand-btn-1')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId('req-expand-btn-1'));
    await waitFor(() => {
      expect(screen.getByTestId('story-list-panel-1')).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText('STR-10')).toBeInTheDocument();
      expect(screen.getByText('STR-11')).toBeInTheDocument();
    });
    expect(screen.getByTestId('stories-new-btn')).toBeInTheDocument();
  });
});
