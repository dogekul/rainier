import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { SprintsPage } from './SprintsPage';
import { useAuthStore } from '../../store/auth';

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
          status: 'ACTIVE',
          requirementId: 1,
          requirementCode: 'REQ-1',
          requirementTitle: '登录流程',
          ownerUserId: 1,
          ownerName: 'Alice',
          ownerLoginName: 'alice',
          storyCount: 2,
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
          id: 100,
          code: 'STR-A',
          title: 'login form',
          status: 'DRAFT',
          priority: 'MEDIUM',
          sprintId: 10,
          ownerUserId: 1,
        },
        {
          id: 101,
          code: 'STR-B',
          title: 'logout button',
          status: 'IN_PROGRESS',
          priority: 'HIGH',
          sprintId: 10,
          ownerUserId: 1,
        },
      ],
      total: 2,
      page: 0,
      size: 100,
    }),
  };
});

describe('SprintsPage', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
  });

  /** TC-FES-SPR-07: 行展开渲染 StoryListPanel(sprintId) + 子表 STR-A/STR-B + 新建按钮. */
  it('row expand renders StoryListPanel keyed by sprintId (TC-FES-SPR-07)', async () => {
    render(<SprintsPage />);
    await waitFor(() => {
      expect(screen.getByText('SPR-A')).toBeInTheDocument();
    });
    expect(screen.getByText('Phase 1')).toBeInTheDocument();
    fireEvent.click(screen.getByTestId('sprint-expand-btn-10'));
    await waitFor(() => {
      expect(screen.getByTestId('story-list-panel-10')).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByText('STR-A')).toBeInTheDocument();
      expect(screen.getByText('STR-B')).toBeInTheDocument();
    });
    expect(screen.getByTestId('stories-new-btn')).toBeInTheDocument();
  });
});
