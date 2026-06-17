import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { TeamLeadPage } from './TeamLeadPage';
import { useAuthStore } from '../../store/auth';
import * as teamApi from '../../api/teamLead';

vi.mock('../../api/teamLead', async (orig) => ({
  ...(await orig<typeof import('../../api/teamLead')>()),
  listLedTeams: vi
    .fn()
    .mockResolvedValue([{ organizationId: 100, organizationName: '采购队', organizationType: 'TEAM' }]),
  listTeamMembers: vi.fn().mockResolvedValue([
    { userId: 1, name: 'Alice', loginName: 'alice' },
    { userId: 2, name: 'Bob', loginName: 'bob' },
  ]),
}));

vi.mock('../../api/task', async (orig) => ({
  ...(await orig<typeof import('../../api/task')>()),
  listTasks: vi.fn((params: { assigneeUserId?: number; projectId?: number }) => {
    const make = (n: number, status: string, dueDate: string | null, base = 0) =>
      Array.from({ length: n }, (_, i) => ({
        id: base + i + 1,
        code: `T${base + i}`,
        title: 't',
        status,
        priority: 'MEDIUM',
        projectId: params.projectId ?? 9,
        dueDate,
      }));
    if (params.assigneeUserId === 1) return Promise.resolve({ content: make(5, 'TODO', null), total: 5, page: 0, size: 200 });
    if (params.assigneeUserId === 2) return Promise.resolve({ content: make(1, 'TODO', null), total: 1, page: 0, size: 200 });
    if (params.projectId === 9)
      return Promise.resolve({
        content: [...make(4, 'TODO', '2020-01-01', 0), ...make(6, 'IN_PROGRESS', '2099-12-31', 10)],
        total: 10, page: 0, size: 200,
      });
    if (params.projectId === 8) return Promise.resolve({ content: make(1, 'DONE', null, 90), total: 1, page: 0, size: 200 });
    return Promise.resolve({ content: [], total: 0, page: 0, size: 200 });
  }),
}));

vi.mock('../../api/story', async (orig) => ({
  ...(await orig<typeof import('../../api/story')>()),
  listStories: vi.fn((params: { ownerUserId?: number }) =>
    Promise.resolve({
      content:
        params.ownerUserId === 1
          ? [
              { id: 1, code: 'S1', title: 's', status: 'DRAFT', priority: 'MEDIUM', sprintId: 1, ownerUserId: 1 },
              { id: 2, code: 'S2', title: 's', status: 'IN_PROGRESS', priority: 'MEDIUM', sprintId: 1, ownerUserId: 1 },
            ]
          : [],
      total: params.ownerUserId === 1 ? 2 : 0, page: 0, size: 200,
    }),
  ),
}));

// v0.0.29: project RYG now comes from GET /api/me/portfolio?scope=led (team footprint), server-sorted.
vi.mock('../../api/portfolio', async (orig) => ({
  ...(await orig<typeof import('../../api/portfolio')>()),
  getPortfolio: vi.fn().mockResolvedValue([
    { projectId: 9, projectCode: 'P9', projectName: '采购', projectStatus: 'ACTIVE', organizationId: 6, openTasks: 10, overdueTasks: 4, blockedTasks: 0, overdueMilestones: 0, ryg: 'RED' },
    { projectId: 8, projectCode: 'P8', projectName: '物流', projectStatus: 'ACTIVE', organizationId: 6, openTasks: 0, overdueTasks: 0, blockedTasks: 0, overdueMilestones: 0, ryg: 'GRAY' },
  ]),
}));

function renderTL() {
  return render(
    <MemoryRouter>
      <TeamLeadPage />
    </MemoryRouter>,
  );
}

describe('TeamLeadPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useAuthStore.setState({
      token: 'tk',
      user: {
        username: 'lead',
        id: 1,
        name: 'Lead',
        roles: [],
        projects: [
          { id: 9, code: 'P9', name: '采购' },
          { id: 8, code: 'P8', name: '物流' },
        ],
      },
    });
  });

  /** TC-TL-01: sole led-team auto-selected; member rows render. */
  it('auto-selects the sole team and shows members (TC-TL-01)', async () => {
    renderTL();
    await waitFor(() => expect(screen.getByTestId('tl-member-1')).toBeInTheDocument());
    expect(screen.getByTestId('tl-member-2')).toBeInTheDocument();
    // single team → no dropdown
    expect(screen.queryByTestId('tl-team-select')).toBeNull();
  });

  /** TC-TL-02: member load bar colored by threshold (5 open → yellow band). */
  it('colors member load by threshold (TC-TL-02)', async () => {
    renderTL();
    await waitFor(() => expect(screen.getByTestId('tl-member-load-1-fill-开放任务')).toBeInTheDocument());
    expect(screen.getByTestId('tl-member-load-1-fill-开放任务')).toHaveAttribute('data-tier', 'yellow');
    // member 2 has 1 open task → green band
    expect(screen.getByTestId('tl-member-load-2-fill-开放任务')).toHaveAttribute('data-tier', 'green');
    expect(screen.getByTestId('tl-member-1')).toHaveTextContent('开放任务 5');
  });

  /** TC-TL-03: project RYG — overdue-ratio 0.4 project is RED and sorted before the GRAY one. */
  it('ranks projects red→gray (TC-TL-03)', async () => {
    renderTL();
    await waitFor(() => expect(screen.getByTestId('tl-project-9')).toBeInTheDocument());
    expect(screen.getByTestId('tl-project-tier-9')).toHaveAttribute('data-tier', 'red');
    expect(screen.getByTestId('tl-project-tier-8')).toHaveAttribute('data-tier', 'gray');
    const html = screen.getByTestId('tl-projects').innerHTML;
    expect(html.indexOf('tl-project-9')).toBeLessThan(html.indexOf('tl-project-8'));
  });

  /** TC-TL-04: drill-through links carry the filter. */
  it('links member and project rows to filtered task lists (TC-TL-04)', async () => {
    renderTL();
    await waitFor(() => expect(screen.getByTestId('tl-member-1')).toBeInTheDocument());
    expect(screen.getByTestId('tl-member-1').querySelector('a')).toHaveAttribute(
      'href',
      '/pm/tasks?assigneeUserId=1',
    );
    expect(screen.getByTestId('tl-project-9').querySelector('a')).toHaveAttribute(
      'href',
      '/pm/tasks?projectId=9',
    );
  });

  /** TC-TL-05: not a lead of any team → empty state. */
  it('shows an empty state when leading no team (TC-TL-05)', async () => {
    (teamApi.listLedTeams as ReturnType<typeof vi.fn>).mockResolvedValueOnce([]);
    renderTL();
    await waitFor(() => expect(screen.getByTestId('team-lead-empty')).toBeInTheDocument());
  });
});
