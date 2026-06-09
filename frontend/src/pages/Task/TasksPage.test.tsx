import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { TasksPage } from './TasksPage';
import { useAuthStore } from '../../store/auth';

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

  /** TC-FES-TSK-002 (partial): /pm/tasks renders TasksPage w/ list + 新建按钮. */
  it('renders task list w/ 新建任务 button', async () => {
    render(<TasksPage />);
    await waitFor(() => {
      expect(screen.getByText('TASK-1')).toBeInTheDocument();
    });
    expect(screen.getByText('修登录页 bug')).toBeInTheDocument();
    expect(screen.getByTestId('task-new-btn')).toBeInTheDocument();
  });
});
