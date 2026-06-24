import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { TasksPage } from './TasksPage';
import { useAuthStore } from '../../store/auth';

vi.mock('../../api/project', async (o) => ({
  ...(await o<typeof import('../../api/project')>()),
  listProjects: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 }),
}));
vi.mock('../../api/sprint', async (o) => ({
  ...(await o<typeof import('../../api/sprint')>()),
  listSprints: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 }),
}));
vi.mock('../../api/story', async (o) => ({
  ...(await o<typeof import('../../api/story')>()),
  listStories: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 }),
}));
vi.mock('../../api/user', async (o) => ({
  ...(await o<typeof import('../../api/user')>()),
  listUsers: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 }),
}));

vi.mock('../../api/task', async () => {
  const actual = await vi.importActual<typeof import('../../api/task')>('../../api/task');
  return {
    ...actual,
    listTasks: vi.fn().mockResolvedValue({
      content: [
        {
          id: 1,
          code: 'TASK-1',
          title: '修登录页 bug',
          status: 'TODO',
          priority: 'MEDIUM',
          projectId: 1,
          projectName: 'Apollo',
          projectCode: 'PROJ-1',
        },
      ],
      total: 1,
      page: 0,
      size: 20,
    }),
  };
});

describe('TasksPage', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
  });

  /** TC-FES-TSK-002 (partial): /pm/tasks renders TasksPage w/ list + 新建按钮 + 中文状态. */
  it('renders task list w/ 新建任务 button and Chinese status label', async () => {
    render(<TasksPage />);
    await waitFor(() => {
      expect(screen.getByText('TASK-1')).toBeInTheDocument();
    });
    expect(screen.getByText('修登录页 bug')).toBeInTheDocument();
    expect(screen.getByTestId('task-new-btn')).toBeInTheDocument();
    // v0.0.50: 任务状态展示中文（TODO → 待办），不再是原始枚举
    expect(screen.getByText('待办')).toBeInTheDocument();
    expect(screen.queryByText('TODO')).not.toBeInTheDocument();
  });

  /** TC-FES-PRIO-001: the task edit drawer's priority dropdown includes 最低 (shared 5-level). */
  it('task drawer priority dropdown includes 最低 (TC-FES-PRIO-001)', async () => {
    render(<TasksPage />);
    await waitFor(() => {
      expect(screen.getByTestId('task-new-btn')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId('task-new-btn'));
    await waitFor(() => {
      expect(screen.getByText('最低')).toBeInTheDocument();
    });
  });
});
