import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CockpitPage } from './CockpitPage';
import { useAuthStore } from '../../store/auth';
import * as taskApi from '../../api/task';
import * as storyApi from '../../api/story';

// Test data uses 2020-01-01 (always overdue) and 2099-12-31 (never overdue) so overdue assertions
// hold regardless of the real system date.

vi.mock('../../api/requirement', async (orig) => ({
  ...(await orig<typeof import('../../api/requirement')>()),
  listRequirements: vi.fn().mockResolvedValue({
    content: [
      { id: 1, code: 'R-1', title: '需求逾期', status: 'IN_PROGRESS', priority: 'MEDIUM', expectedDate: '2020-01-01' },
      { id: 2, code: 'R-2', title: '需求完成', status: 'DELIVERED', priority: 'MEDIUM', expectedDate: '2020-01-01' },
    ],
    total: 2, page: 0, size: 200,
  }),
}));
vi.mock('../../api/sprint', async (orig) => ({
  ...(await orig<typeof import('../../api/sprint')>()),
  listSprints: vi.fn().mockResolvedValue({
    content: [{ id: 1, code: 'S-1', name: '迭代A', status: 'ACTIVE', requirementId: 1, ownerUserId: 5, endDate: '2099-12-31' }],
    total: 1, page: 0, size: 200,
  }),
}));
vi.mock('../../api/milestone', async (orig) => ({
  ...(await orig<typeof import('../../api/milestone')>()),
  listMilestones: vi.fn().mockResolvedValue({
    content: [{ id: 1, projectId: 9, code: 'M-1', name: '里程碑逾期', status: 'PLANNED', targetDate: '2020-01-01', sortOrder: 0 }],
    total: 1, page: 0, size: 200,
  }),
}));
vi.mock('../../api/story', async (orig) => ({
  ...(await orig<typeof import('../../api/story')>()),
  listStories: vi.fn().mockResolvedValue({
    content: [
      { id: 11, code: 'ST-1', title: '待评审Story', status: 'DRAFT', priority: 'MEDIUM', sprintId: 1, ownerUserId: 5 },
      { id: 12, code: 'ST-2', title: '完成Story', status: 'DONE', priority: 'MEDIUM', sprintId: 1, ownerUserId: 5 },
    ],
    total: 2, page: 0, size: 200,
  }),
  updateStory: vi.fn().mockResolvedValue({ id: 11 }),
}));
vi.mock('../../api/task', async (orig) => ({
  ...(await orig<typeof import('../../api/task')>()),
  listTasks: vi.fn().mockResolvedValue({
    content: [
      { id: 21, code: 'T-1', title: '逾期待办', status: 'TODO', priority: 'MEDIUM', projectId: 9, dueDate: '2020-01-01' },
      { id: 22, code: 'T-2', title: '已完成', status: 'DONE', priority: 'MEDIUM', projectId: 9, dueDate: '2020-01-01' },
    ],
    total: 2, page: 0, size: 200,
  }),
  updateTask: vi.fn().mockResolvedValue({ id: 21 }),
}));

vi.mock('../../api/portfolio', async (orig) => ({
  ...(await orig<typeof import('../../api/portfolio')>()),
  getPortfolio: vi.fn().mockResolvedValue([
    { projectId: 9, projectCode: 'PRJ-9', projectName: '采购', projectStatus: 'ACTIVE', organizationId: null, openTasks: 5, overdueTasks: 2, blockedTasks: 1, overdueMilestones: 0, ryg: 'RED' },
    { projectId: 8, projectCode: 'PRJ-8', projectName: '物流', projectStatus: 'ACTIVE', organizationId: null, openTasks: 3, overdueTasks: 0, blockedTasks: 0, overdueMilestones: 0, ryg: 'GREEN' },
  ]),
}));

function renderCockpit() {
  return render(
    <MemoryRouter>
      <CockpitPage />
    </MemoryRouter>,
  );
}

describe('CockpitPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({
      token: 'tk',
      user: {
        username: 'pm',
        id: 5,
        name: 'PM',
        roles: [],
        projects: [
          { id: 9, code: 'PRJ-9', name: '采购' },
          { id: 8, code: 'PRJ-8', name: '物流' },
        ],
      },
    });
  });

  /** TC-COCK-01: smart-defaults to the first project and loads all 5 distribution cards. */
  it('auto-selects the first project and loads 5 cards (TC-COCK-01)', async () => {
    renderCockpit();
    const sel = screen.getByTestId('cockpit-project-select') as HTMLSelectElement;
    expect(sel.value).toBe('9');
    await waitFor(() => expect(screen.getByTestId('cockpit-card-task')).toBeInTheDocument());
    for (const k of ['requirement', 'sprint', 'story', 'task', 'milestone']) {
      expect(screen.getByTestId(`cockpit-card-${k}`)).toBeInTheDocument();
    }
    const { listTasks } = taskApi;
    expect(listTasks).toHaveBeenCalledWith(expect.objectContaining({ projectId: 9, size: 200 }));
  });

  /** TC-COCK-02: task distribution shows a segment per status with the DONE-tier green. */
  it('renders the task status distribution (TC-COCK-02)', async () => {
    renderCockpit();
    await waitFor(() => expect(screen.getByTestId('dist-task-fill-TODO')).toBeInTheDocument());
    expect(screen.getByTestId('dist-task-fill-TODO')).toHaveAttribute('data-tier', 'yellow');
    expect(screen.getByTestId('dist-task-fill-DONE')).toHaveAttribute('data-tier', 'green');
  });

  /** TC-COCK-03: overdue task/milestone/requirement listed; DONE/DELIVERED excluded. */
  it('lists overdue items and excludes completed ones (TC-COCK-03)', async () => {
    renderCockpit();
    await waitFor(() => expect(screen.getByTestId('cockpit-overdue')).toBeInTheDocument());
    expect(screen.getByTestId('cockpit-overdue-task-21')).toBeInTheDocument();
    expect(screen.getByTestId('cockpit-overdue-milestone-1')).toBeInTheDocument();
    expect(screen.getByTestId('cockpit-overdue-requirement-1')).toBeInTheDocument();
    // DONE task / DELIVERED requirement / future-dated sprint are NOT overdue
    expect(screen.queryByTestId('cockpit-overdue-task-22')).toBeNull();
    expect(screen.queryByTestId('cockpit-overdue-requirement-2')).toBeNull();
    expect(screen.queryByTestId('cockpit-overdue-sprint-1')).toBeNull();
  });

  /** TC-COCK-04: inline task status change calls updateTask with the full body. */
  it('inline-changes a 待办 task status (TC-COCK-04)', async () => {
    renderCockpit();
    await waitFor(() => expect(screen.getByTestId('cockpit-todo-21')).toBeInTheDocument());
    fireEvent.change(screen.getByTestId('cockpit-todo-status-21'), { target: { value: 'IN_PROGRESS' } });
    await waitFor(() =>
      expect(taskApi.updateTask).toHaveBeenCalledWith(
        21,
        expect.objectContaining({ status: 'IN_PROGRESS', code: 'T-1', title: '逾期待办' }),
      ),
    );
  });

  /** TC-COCK-05: 待评审 lists DRAFT stories; inline change calls updateStory. */
  it('lists 待评审 stories and inline-changes one (TC-COCK-05)', async () => {
    renderCockpit();
    await waitFor(() => expect(screen.getByTestId('cockpit-review-11')).toBeInTheDocument());
    expect(screen.queryByTestId('cockpit-review-12')).toBeNull(); // DONE story not in review
    fireEvent.change(screen.getByTestId('cockpit-review-status-11'), { target: { value: 'READY' } });
    await waitFor(() =>
      expect(storyApi.updateStory).toHaveBeenCalledWith(
        11,
        expect.objectContaining({ status: 'READY', ownerUserId: 5 }),
      ),
    );
  });

  /** TC-COCK-06: switching the project reloads scoped to the new project. */
  it('reloads when the project picker changes (TC-COCK-06)', async () => {
    renderCockpit();
    await waitFor(() => expect(taskApi.listTasks).toHaveBeenCalledWith(expect.objectContaining({ projectId: 9 })));
    fireEvent.change(screen.getByTestId('cockpit-project-select'), { target: { value: '8' } });
    await waitFor(() => expect(taskApi.listTasks).toHaveBeenCalledWith(expect.objectContaining({ projectId: 8 })));
  });

  /** TC-COCK-08: cross-project strip renders worst-first; clicking a card drills into that project. */
  it('renders the cross-project health strip and drills in on click (TC-COCK-08)', async () => {
    renderCockpit();
    await waitFor(() => expect(screen.getByTestId('cockpit-portfolio')).toBeInTheDocument());
    expect(screen.getByTestId('cockpit-portfolio-ryg-9')).toHaveAttribute('data-tier', 'red');
    expect(screen.getByTestId('cockpit-portfolio-ryg-8')).toHaveAttribute('data-tier', 'green');
    // click the 物流 (id 8) card → cockpit selects project 8 + reloads scoped to it
    fireEvent.click(screen.getByTestId('cockpit-portfolio-8'));
    await waitFor(() =>
      expect(taskApi.listTasks).toHaveBeenCalledWith(expect.objectContaining({ projectId: 8 })),
    );
  });

  /** TC-COCK-07: no projects → friendly empty state, no list calls. */
  it('shows an empty state when the user has no projects (TC-COCK-07)', async () => {
    useAuthStore.setState({ token: 'tk', user: { username: 'x', id: 1, roles: [], projects: [] } });
    renderCockpit();
    expect(screen.getByTestId('cockpit-empty')).toBeInTheDocument();
    expect(taskApi.listTasks).not.toHaveBeenCalled();
  });
});
