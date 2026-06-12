import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { MilestonesPanel } from './MilestonesPanel';
import * as milestoneApi from '../../api/milestone';

vi.mock('../../api/milestone', async () => {
  const actual =
    await vi.importActual<typeof import('../../api/milestone')>('../../api/milestone');
  return {
    ...actual,
    listMilestones: vi.fn().mockResolvedValue({
      content: [
        {
          id: 11,
          projectId: 7,
          code: 'M-1',
          name: '需求评审',
          description: null,
          targetDate: '2026-07-01',
          status: 'PLANNED',
          actualDate: null,
          sortOrder: 0,
        },
        {
          id: 12,
          projectId: 7,
          code: 'M-2',
          name: '上线',
          description: null,
          targetDate: '2026-08-01',
          status: 'REACHED',
          actualDate: '2026-07-30',
          sortOrder: 1,
        },
      ],
      total: 2,
      page: 0,
      size: 100,
    }),
    createMilestone: vi.fn().mockResolvedValue({ id: 99 }),
    deleteMilestone: vi.fn().mockResolvedValue(undefined),
  };
});

describe('MilestonesPanel', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  /** TC-FES-MILE-002: 面板列出该项目里程碑（name + status）. */
  it('lists the project milestones (TC-FES-MILE-002)', async () => {
    render(<MilestonesPanel projectId={7} />);
    await waitFor(() => {
      expect(screen.getByText('需求评审')).toBeInTheDocument();
    });
    expect(milestoneApi.listMilestones).toHaveBeenCalledWith(
      expect.objectContaining({ projectId: 7 }),
    );
    expect(screen.getByText('上线')).toBeInTheDocument();
    // status 中文标签 — scope to the row so the form's status <option> doesn't satisfy it.
    expect(within(screen.getByTestId('milestone-row-11')).getByText('计划中')).toBeInTheDocument();
    expect(within(screen.getByTestId('milestone-row-12')).getByText('已达成')).toBeInTheDocument();
  });

  /** TC-FES-MILE-003: 面板新建里程碑携带 projectId. */
  it('creates a milestone carrying projectId (TC-FES-MILE-003)', async () => {
    render(<MilestonesPanel projectId={7} />);
    await waitFor(() => {
      expect(screen.getByTestId('milestone-name-input')).toBeInTheDocument();
    });
    fireEvent.change(screen.getByTestId('milestone-code-input'), { target: { value: 'M-9' } });
    fireEvent.change(screen.getByTestId('milestone-name-input'), { target: { value: '新里程碑' } });
    fireEvent.change(screen.getByTestId('milestone-targetdate-input'), {
      target: { value: '2026-09-01' },
    });
    fireEvent.click(screen.getByTestId('milestone-save-btn'));
    await waitFor(() => {
      expect(milestoneApi.createMilestone).toHaveBeenCalledWith(
        expect.objectContaining({
          projectId: 7,
          code: 'M-9',
          name: '新里程碑',
          targetDate: '2026-09-01',
        }),
      );
    });
  });

  /** TC-FES-MILE-004: 面板删除里程碑. */
  it('deletes a milestone by id (TC-FES-MILE-004)', async () => {
    render(<MilestonesPanel projectId={7} />);
    await waitFor(() => {
      expect(screen.getByTestId('milestone-delete-btn-11')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId('milestone-delete-btn-11'));
    await waitFor(() => {
      expect(milestoneApi.deleteMilestone).toHaveBeenCalledWith(11);
    });
  });
});
