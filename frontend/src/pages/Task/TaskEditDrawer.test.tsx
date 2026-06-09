import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { TaskEditDrawer } from './TaskEditDrawer';
import { useAuthStore } from '../../store/auth';

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
      ],
      total: 2,
      page: 0,
      size: 100,
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
  });

  /** TC-FES-TSK-003: Project 切换后 Sprint/Story 选择清空 + Sprint 下拉项按 project 过滤. */
  it('clears Sprint/Story when Project changes and filters Sprint options (TC-FES-TSK-003)', async () => {
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
    // Pick Project A; Sprint A (id=10, projectId=1) should be selectable
    fireEvent.change(screen.getByTestId('task-project-select'), { target: { value: '1' } });
    await waitFor(() => {
      const sprintSelect = screen.getByTestId('task-sprint-select') as HTMLSelectElement;
      const options = Array.from(sprintSelect.options).map((o) => o.value);
      expect(options).toContain('10'); // Sprint A in Project A
      expect(options).not.toContain('20'); // Sprint B is in Project B
    });
    // Pick Sprint 10
    fireEvent.change(screen.getByTestId('task-sprint-select'), { target: { value: '10' } });
    expect((screen.getByTestId('task-sprint-select') as HTMLSelectElement).value).toBe('10');

    // Switch Project A → B, expect Sprint + Story cleared
    fireEvent.change(screen.getByTestId('task-project-select'), { target: { value: '2' } });
    await waitFor(() => {
      expect((screen.getByTestId('task-sprint-select') as HTMLSelectElement).value).toBe('');
      expect((screen.getByTestId('task-story-select') as HTMLSelectElement).value).toBe('');
    });
    // Sprint B (id=20) should now be available
    const optionsAfter = Array.from(
      (screen.getByTestId('task-sprint-select') as HTMLSelectElement).options,
    ).map((o) => o.value);
    expect(optionsAfter).toContain('20');
    expect(optionsAfter).not.toContain('10');
  });
});
