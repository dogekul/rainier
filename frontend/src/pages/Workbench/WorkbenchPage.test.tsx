import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { WorkbenchPage } from './WorkbenchPage';
import { useAuthStore } from '../../store/auth';
import * as taskApi from '../../api/task';

vi.mock('../../api/auth', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/auth')>();
  return {
    ...actual,
    me: vi.fn().mockResolvedValue({
      id: 5,
      username: 'alice',
      name: 'Alice',
      roles: [
        {
          roleId: 1,
          roleCode: 'PMO',
          roleName: 'PMO',
          projectId: 9,
          projectName: '采购',
          projectCode: 'PRJ-1',
        },
      ],
      projects: [{ id: 9, code: 'PRJ-1', name: '采购' }],
    }),
  };
});

vi.mock('../../api/task', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/task')>();
  return {
    ...actual,
    listTasks: vi.fn().mockResolvedValue({
      content: [
        {
          id: 11,
          code: 'T-1',
          title: '我的任务A',
          status: 'TODO',
          priority: 'MEDIUM',
          projectId: 9,
          projectName: '采购',
          assigneeUserId: 5,
        },
      ],
      total: 1,
      page: 0,
      size: 100,
    }),
    updateTask: vi.fn().mockResolvedValue({ id: 11 }),
  };
});

vi.mock('../../api/story', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/story')>();
  return {
    ...actual,
    listStories: vi.fn().mockResolvedValue({
      content: [
        {
          id: 21,
          code: 'S-1',
          title: '我的StoryA',
          status: 'DRAFT',
          priority: 'MEDIUM',
          sprintId: 1,
          ownerUserId: 5,
        },
      ],
      total: 1,
      page: 0,
      size: 100,
    }),
  };
});

describe('WorkbenchPage', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
    vi.clearAllMocks();
  });

  /** TC-FES-WB-001: 渲染问候 + 角色 + 三块. */
  it('renders greeting, roles and my work (TC-FES-WB-001)', async () => {
    render(
      <MemoryRouter>
        <WorkbenchPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('workbench-greeting')).toHaveTextContent('Alice');
    });
    expect(screen.getByTestId('workbench-roles')).toHaveTextContent('PMO');
    await waitFor(() => {
      expect(screen.getByTestId('my-task-11')).toBeInTheDocument();
    });
    expect(screen.getByTestId('my-story-21')).toBeInTheDocument();
    expect(screen.getByTestId('my-project-9')).toBeInTheDocument();
  });

  /** TC-FES-WB-002: 我的任务/Story 携当前用户 id 查询. */
  it('queries my work with the current user id (TC-FES-WB-002)', async () => {
    const { listTasks } = await import('../../api/task');
    const { listStories } = await import('../../api/story');
    render(
      <MemoryRouter>
        <WorkbenchPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(listTasks).toHaveBeenCalledWith(expect.objectContaining({ assigneeUserId: 5 }));
    });
    expect(listStories).toHaveBeenCalledWith(expect.objectContaining({ ownerUserId: 5 }));
  });

  /** TC-FES-WB-003: 状态快改触发 updateTask. */
  it('changes a task status via updateTask (TC-FES-WB-003)', async () => {
    render(
      <MemoryRouter>
        <WorkbenchPage />
      </MemoryRouter>,
    );
    await waitFor(() => {
      expect(screen.getByTestId('my-task-status-11')).toBeInTheDocument();
    });
    fireEvent.change(screen.getByTestId('my-task-status-11'), {
      target: { value: 'IN_PROGRESS' },
    });
    await waitFor(() => {
      expect(taskApi.updateTask).toHaveBeenCalledWith(
        11,
        expect.objectContaining({ status: 'IN_PROGRESS' }),
      );
    });
  });
});
