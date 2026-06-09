import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { TaskEditDrawer } from './TaskEditDrawer';
import { useAuthStore } from '../../store/auth';
import * as sprintApi from '../../api/sprint';
import * as storyApi from '../../api/story';

vi.mock('../../api/project', async () => {
  const actual = await vi.importActual<typeof import('../../api/project')>('../../api/project');
  return {
    ...actual,
    listProjects: vi.fn().mockResolvedValue({
      content: [
        { id: 1, code: 'PROJ-A', name: 'Apollo', status: 'ACTIVE', ownerUserId: 1, enabled: true },
        { id: 2, code: 'PROJ-B', name: 'Beta', status: 'ACTIVE', ownerUserId: 1, enabled: true },
      ],
      total: 2,
      page: 0,
      size: 100,
    }),
  };
});

// v0.0.12.1 A2: listSprints called with projectId — mock filters accordingly.
vi.mock('../../api/sprint', async () => {
  const actual = await vi.importActual<typeof import('../../api/sprint')>('../../api/sprint');
  const ALL_SPRINTS = [
    {
      id: 10,
      code: 'SPR-A',
      name: 'Phase 1',
      status: 'ACTIVE',
      requirementId: 1,
      ownerUserId: 1,
      projectId: 1,
    },
    {
      id: 20,
      code: 'SPR-B',
      name: 'Phase 2',
      status: 'ACTIVE',
      requirementId: 2,
      ownerUserId: 1,
      projectId: 2,
    },
  ];
  return {
    ...actual,
    listSprints: vi.fn().mockImplementation((params: { projectId?: number } = {}) => {
      const filtered = params.projectId
        ? ALL_SPRINTS.filter((s) => s.projectId === params.projectId)
        : ALL_SPRINTS;
      return Promise.resolve({ content: filtered, total: filtered.length, page: 0, size: 100 });
    }),
  };
});

vi.mock('../../api/story', async () => {
  const actual = await vi.importActual<typeof import('../../api/story')>('../../api/story');
  return {
    ...actual,
    listStories: vi.fn().mockResolvedValue({
      content: [],
      total: 0,
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
      content: [{ id: 1, loginName: 'alice', name: 'Alice', isInternal: true, enabled: true }],
      total: 1,
      page: 0,
      size: 100,
    }),
  };
});

describe('TaskEditDrawer', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
    vi.clearAllMocks();
  });

  /**
   * TC-FES-TSK-003 (v0.0.12.1 A2 — server-side filter):
   * Project 切换 → listSprints/listStories 服务器侧 projectId 过滤 + Sprint/Story 清空.
   */
  it(
    'fetches Sprints/Stories server-filtered by projectId when Project changes (TC-FES-TSK-003)',
    async () => {
      render(
        <TaskEditDrawer
          open={true}
          editing={null}
          onClose={vi.fn()}
          onCreate={vi.fn()}
          onUpdate={vi.fn()}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId('task-project-select')).toBeInTheDocument();
      });

      // No Project picked → no Sprint/Story API call yet.
      expect(sprintApi.listSprints).not.toHaveBeenCalled();
      expect(storyApi.listStories).not.toHaveBeenCalled();

      // Pick Project 1 → listSprints({projectId:1}) + listStories({projectId:1}).
      fireEvent.change(screen.getByTestId('task-project-select'), { target: { value: '1' } });
      await waitFor(() => {
        expect(sprintApi.listSprints).toHaveBeenCalledWith({ projectId: 1, size: 100 });
      });
      await waitFor(() => {
        expect(storyApi.listStories).toHaveBeenCalledWith({ projectId: 1, size: 100 });
      });
      await waitFor(() => {
        const sprintSelect = screen.getByTestId('task-sprint-select') as HTMLSelectElement;
        const options = Array.from(sprintSelect.options).map((o) => o.value);
        expect(options).toContain('10'); // Sprint A in Project 1
        expect(options).not.toContain('20'); // Sprint B in Project 2 — filtered out
      });

      // Pick Sprint 10 → listStories now narrower by sprintId.
      fireEvent.change(screen.getByTestId('task-sprint-select'), { target: { value: '10' } });
      await waitFor(() => {
        expect(storyApi.listStories).toHaveBeenLastCalledWith({ sprintId: 10, size: 100 });
      });

      // Switch Project 1 → 2: Sprint + Story cleared.
      fireEvent.change(screen.getByTestId('task-project-select'), { target: { value: '2' } });
      await waitFor(() => {
        expect((screen.getByTestId('task-sprint-select') as HTMLSelectElement).value).toBe('');
        expect((screen.getByTestId('task-story-select') as HTMLSelectElement).value).toBe('');
      });
      await waitFor(() => {
        expect(sprintApi.listSprints).toHaveBeenLastCalledWith({ projectId: 2, size: 100 });
      });
      const optionsAfter = Array.from(
        (screen.getByTestId('task-sprint-select') as HTMLSelectElement).options,
      ).map((o) => o.value);
      expect(optionsAfter).toContain('20');
      expect(optionsAfter).not.toContain('10');
    },
  );
});
