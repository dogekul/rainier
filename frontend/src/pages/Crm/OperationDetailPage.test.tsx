import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OperationDetailPage from './OperationDetailPage';
import { getOperation, updateOperation, type Operation } from '../../api/operation';
import {
  convertOperationIssueToTask,
  listOperationIssues,
  type OperationIssue,
} from '../../api/operationIssue';
import { listProjects, type Project } from '../../api/project';
import { listUsers } from '../../api/user';
import { listDemands } from '../../api/demand';
import { listRequirements } from '../../api/requirement';
import { listOpportunityArtifacts } from '../../api/opportunityArtifact';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async (orig) => ({
  ...(await orig<typeof import('react-router-dom')>()),
  useNavigate: () => mockNavigate,
}));

vi.mock('../../store/auth', () => ({
  useAuthStore: (selector: (s: { user: { id: number; username: string } }) => unknown) =>
    selector({ user: { id: 5, username: 'alice' } }),
}));

vi.mock('../../api/operation', async (orig) => ({
  ...(await orig<typeof import('../../api/operation')>()),
  getOperation: vi.fn(),
  updateOperation: vi.fn(() => Promise.resolve({} as Operation)),
}));

vi.mock('../../api/operationIssue', async (orig) => ({
  ...(await orig<typeof import('../../api/operationIssue')>()),
  listOperationIssues: vi.fn(),
  convertOperationIssueToTask: vi.fn(),
}));

vi.mock('../../api/project', async (orig) => ({
  ...(await orig<typeof import('../../api/project')>()),
  listProjects: vi.fn(),
}));

vi.mock('../../api/user', () => ({
  listUsers: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 }),
}));

vi.mock('../../api/demand', () => ({
  listDemands: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 }),
}));

vi.mock('../../api/requirement', () => ({
  listRequirements: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 }),
}));

vi.mock('../../api/opportunityArtifact', async (orig) => ({
  ...(await orig<typeof import('../../api/opportunityArtifact')>()),
  listOpportunityArtifacts: vi.fn().mockResolvedValue([]),
  exportArtifactDocx: vi.fn(),
}));

function op(over: Partial<Operation> = {}): Operation {
  return {
    id: 6,
    customerName: '远方集团',
    title: '运维续约',
    stage: 'MAINTENANCE',
    status: 'ACTIVE',
    opsOwnerUserId: 5,
    opsOwnerName: 'Alice',
    projectId: 101,
    opportunityId: null,
    ...over,
  };
}

function issue(over: Partial<OperationIssue> = {}): OperationIssue {
  return {
    id: 33,
    operationId: 6,
    title: '现场问题',
    description: '客户反馈',
    severity: 'HIGH',
    status: 'OPEN',
    reporterUserId: 5,
    reporterName: 'Alice',
    ...over,
  };
}

function project(id: number, name: string): Project {
  return {
    id,
    code: `P-${id}`,
    name,
    status: 'ACTIVE',
    projectType: 'EXTERNAL_DELIVERY',
    ownerUserId: 5,
    enabled: true,
  };
}

function renderAt(id = 6) {
  return render(
    <MemoryRouter initialEntries={[`/crm/operations/${id}`]}>
      <Routes>
        <Route path="/crm/operations/:id" element={<OperationDetailPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('OperationDetailPage', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    vi.spyOn(window, 'prompt').mockReturnValue(null);
    vi.spyOn(window, 'alert').mockImplementation(() => undefined);
    vi.mocked(getOperation).mockReset().mockResolvedValue(op());
    vi.mocked(updateOperation).mockClear();
    vi.mocked(listUsers).mockClear();
    vi.mocked(listDemands).mockClear();
    vi.mocked(listRequirements).mockClear();
    vi.mocked(listOpportunityArtifacts).mockClear();
    vi.mocked(listOperationIssues).mockReset().mockResolvedValue([issue()]);
    vi.mocked(listProjects).mockReset().mockResolvedValue({
      content: [project(101, '交付项目'), project(202, '增购项目')],
      total: 2,
      page: 0,
      size: 200,
    });
    vi.mocked(convertOperationIssueToTask)
      .mockReset()
      .mockResolvedValue({ id: 77, code: 'T-OPI-33', title: '现场问题', projectId: 202 });
  });

  /** TC-OPICK-01: 转工单时选择真实项目，不再手输 projectId。 */
  it('converts an operation issue through a project picker instead of prompt (TC-OPICK-01)', async () => {
    renderAt();

    await waitFor(() => expect(screen.getByTestId('op-issue-convert-33')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('op-issue-convert-33'));

    await waitFor(() => expect(screen.getByTestId('op-issue-convert-panel-33')).toBeInTheDocument());
    expect(window.prompt).not.toHaveBeenCalled();
    expect(listProjects).toHaveBeenCalledWith({ size: 200 });

    const select = screen.getByTestId('op-issue-convert-project-33') as HTMLSelectElement;
    expect(select.value).toBe('101');
    fireEvent.change(select, { target: { value: '202' } });
    fireEvent.click(screen.getByTestId('op-issue-convert-submit-33'));

    await waitFor(() => expect(convertOperationIssueToTask).toHaveBeenCalledWith(33, 202));
    await waitFor(() => expect(screen.queryByTestId('op-issue-convert-panel-33')).not.toBeInTheDocument());
  });
});
