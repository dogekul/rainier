import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { WorkbenchPage } from './WorkbenchPage';
import { useAuthStore } from '../../store/auth';
import * as taskApi from '../../api/task';

// v0.0.20: WorkbenchPage now reads the current-user context from the store (hydrated app-wide by
// ProtectedRoute), so the test seeds the store directly — no me() mock needed here.
const STORE_USER = {
  username: 'alice',
  id: 5,
  name: 'Alice',
  roles: [
    {
      roleId: 1,
      roleCode: 'PMO',
      roleName: 'PMO',
      projectId: 9,
      projectName: '采购',
      projectCode: 'PRJ-1',
      adminAccess: false,
    },
  ],
  projects: [{ id: 9, code: 'PRJ-1', name: '采购' }],
};

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
        {
          id: 12,
          code: 'T-2',
          title: '逾期任务',
          status: 'TODO',
          priority: 'HIGH',
          projectId: 9,
          projectName: '采购',
          assigneeUserId: 5,
          dueDate: '2020-01-01',
        },
      ],
      total: 2,
      page: 0,
      size: 100,
    }),
    updateTask: vi.fn().mockResolvedValue({ id: 11 }),
  };
});

// F4 — WorkbenchPage now embeds <AiSuggestionCard /> which loads PROPOSED AiWorkLogs on mount.
// Stub the API so unrelated workbench tests don't see network noise.
vi.mock('../../api/aiWorkLog', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../../api/aiWorkLog')>();
  return {
    ...actual,
    listMyProposals: vi.fn().mockResolvedValue([
      {
        id: 901,
        agentType: 'STATUS_SYNC',
        action: 'UPDATE_TASK_STATUS',
        summary: '建议关闭任务 #1',
        evidence: '{}',
        status: 'PROPOSED',
      },
    ]),
    acceptWorkLog: vi.fn(),
    rejectWorkLog: vi.fn(),
    reverseWorkLog: vi.fn(),
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
    useAuthStore.setState({ token: 'tk', user: STORE_USER });
    vi.clearAllMocks();
  });

  /** TC-FES-WB-001 / TC-FES-RN-009: 渲染问候 + 角色 + 三块（数据来自 store）. */
  it('renders greeting, roles and my work from the store (TC-FES-WB-001 / TC-FES-RN-009)', async () => {
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

  /** TC-WB-FOCUS: 今日聚焦 — overdue task flagged 逾期 and sorted above the no-due task. */
  it('flags and sorts overdue tasks to the top (TC-WB-FOCUS)', async () => {
    render(
      <MemoryRouter>
        <WorkbenchPage />
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByTestId('my-task-12')).toBeInTheDocument());
    expect(screen.getByTestId('my-task-flag-12')).toHaveTextContent('逾期');
    // overdue task (12) renders before the no-due task (11)
    const html = screen.getByTestId('wb-tasks').innerHTML;
    expect(html.indexOf('my-task-12')).toBeLessThan(html.indexOf('my-task-11'));
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

  /** TC-WB-AI-CARD: F4 workbench embeds the AI suggestion card and renders the seeded row. */
  it('embeds the AI suggestion card (F4 TC-WB-AI-CARD)', async () => {
    render(
      <MemoryRouter>
        <WorkbenchPage />
      </MemoryRouter>,
    );
    await waitFor(() =>
      expect(screen.getByTestId('ai-suggest-card')).toBeInTheDocument(),
    );
    await waitFor(() =>
      expect(screen.getByTestId('ai-suggest-row-901')).toBeInTheDocument(),
    );
  });
});
