import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { RequirementEditDrawer } from './RequirementEditDrawer';

vi.mock('../../api/demand', async () => {
  const actual = await vi.importActual<typeof import('../../api/demand')>('../../api/demand');
  return {
    ...actual,
    listDemands: vi.fn().mockResolvedValue({
      content: [
        {
          id: 10,
          title: 'demand10',
          submitterUserId: 1,
          status: 'PENDING',
          priority: 'MEDIUM',
          source: 'WEB',
        },
        {
          id: 20,
          title: 'demand20',
          submitterUserId: 1,
          status: 'PENDING',
          priority: 'MEDIUM',
          source: 'WEB',
        },
      ],
      total: 2,
      page: 0,
      size: 10,
    }),
  };
});

vi.mock('../../api/user', async () => {
  const actual = await vi.importActual<typeof import('../../api/user')>('../../api/user');
  return {
    ...actual,
    listUsers: vi.fn().mockResolvedValue({
      content: [
        { id: 1, loginName: 'alice', name: 'Alice', isInternal: true, enabled: true },
        { id: 2, loginName: 'lili', name: '黎立', isInternal: true, enabled: true },
      ],
      total: 2,
      page: 0,
      size: 100,
    }),
  };
});

vi.mock('../../api/project', async () => {
  const actual = await vi.importActual<typeof import('../../api/project')>('../../api/project');
  return {
    ...actual,
    listProjects: vi.fn().mockResolvedValue({
      content: [
        {
          id: 5,
          code: 'PROJ-5',
          name: 'Apollo',
          status: 'ACTIVE',
          ownerUserId: 1,
          enabled: true,
        },
        {
          id: 7,
          code: 'PROJ-7',
          name: 'Borealis',
          status: 'PLANNING',
          ownerUserId: 1,
          enabled: true,
        },
      ],
      total: 2,
      page: 0,
      size: 100,
    }),
  };
});

describe('RequirementEditDrawer', () => {
  /** TC-FES-D03: 用户勾选 2 个 demand 后保存 → onCreate 收到 body.sourceDemandIds = [10, 20]. */
  it('passes selected demand ids and selected projectId on create (TC-FES-D03 + v0.0.8 project)', async () => {
    const onCreate = vi.fn();
    const onUpdate = vi.fn();
    render(
      <RequirementEditDrawer
        open={true}
        editing={null}
        onClose={vi.fn()}
        onCreate={onCreate}
        onUpdate={onUpdate}
      />,
    );

    // Wait for the source-demands sub-area to render after listDemands resolves.
    await waitFor(() => {
      expect(screen.getByTestId('req-sources-section')).toBeInTheDocument();
      expect(screen.getByTestId('req-source-checkbox-10')).toBeInTheDocument();
    });

    // Fill required text fields.
    const codeInput = screen.getByLabelText(/编码/);
    fireEvent.change(codeInput, { target: { value: 'REQ-CONV-1' } });
    const titleInput = screen.getByLabelText(/标题/);
    fireEvent.change(titleInput, { target: { value: 'X' } });

    // Pick owner.
    fireEvent.change(screen.getByTestId('req-owner-select'), { target: { value: '1' } });

    // v0.0.8: pick a project from the dropdown (replaces old numeric input).
    await waitFor(() => {
      expect(screen.getByTestId('req-project-select')).toBeInTheDocument();
    });
    fireEvent.change(screen.getByTestId('req-project-select'), { target: { value: '5' } });

    // Tick both demands.
    fireEvent.click(screen.getByTestId('req-source-checkbox-10'));
    fireEvent.click(screen.getByTestId('req-source-checkbox-20'));

    // Click save.
    fireEvent.click(screen.getByRole('button', { name: '保存' }));

    await waitFor(() => {
      expect(onCreate).toHaveBeenCalledTimes(1);
    });
    const body = onCreate.mock.calls[0][0];
    expect(body.sourceDemandIds).toEqual([10, 20]);
    expect(body.code).toBe('REQ-CONV-1');
    expect(body.title).toBe('X');
    expect(body.ownerUserId).toBe(1);
    expect(body.projectId).toBe(5);
  });

  /** v0.0.8 TC-FES-P-REQ-EDIT: edit drawer allows changing owner; update body carries new ownerUserId. */
  it('allows changing owner in edit mode and sends new ownerUserId on save', async () => {
    const onUpdate = vi.fn();
    render(
      <RequirementEditDrawer
        open={true}
        editing={{
          id: 99,
          code: 'REQ-1',
          title: 'T',
          ownerUserId: 1,
          status: 'DRAFT',
          priority: 'MEDIUM',
        }}
        onClose={vi.fn()}
        onCreate={vi.fn()}
        onUpdate={onUpdate}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId('req-owner-select')).not.toBeDisabled();
    });
    fireEvent.change(screen.getByTestId('req-owner-select'), { target: { value: '2' } });
    fireEvent.click(screen.getByRole('button', { name: '保存' }));
    await waitFor(() => {
      expect(onUpdate).toHaveBeenCalledTimes(1);
    });
    const [id, body] = onUpdate.mock.calls[0];
    expect(id).toBe(99);
    expect(body.ownerUserId).toBe(2);
  });

  /** TC-FES-REQE-001: status dropdown shows the new 6 中文 states, not the legacy labels. */
  it('renders the new 6 status options in Chinese (TC-FES-REQE-001)', async () => {
    render(
      <RequirementEditDrawer
        open={true}
        editing={null}
        onClose={vi.fn()}
        onCreate={vi.fn()}
        onUpdate={vi.fn()}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId('req-sources-section')).toBeInTheDocument();
    });
    for (const label of ['草稿', '审批中', '分析中', '实施中', '已交付', '已关闭']) {
      expect(screen.getByText(label)).toBeInTheDocument();
    }
    // legacy labels gone
    expect(screen.queryByText('评审中')).toBeNull();
    expect(screen.queryByText('已批准')).toBeNull();
    expect(screen.queryByText('已废弃')).toBeNull();
  });

  /** TC-FES-REQE-002: priority dropdown has 5 levels including 最低. */
  it('renders 5 priority options including 最低 (TC-FES-REQE-002)', async () => {
    render(
      <RequirementEditDrawer
        open={true}
        editing={null}
        onClose={vi.fn()}
        onCreate={vi.fn()}
        onUpdate={vi.fn()}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId('req-sources-section')).toBeInTheDocument();
    });
    expect(screen.getByText('最低')).toBeInTheDocument();
    expect(screen.getByText('紧急')).toBeInTheDocument();
  });

  /** TC-FES-REQE-003: submitting carries expectedDate. */
  it('submits expectedDate on create (TC-FES-REQE-003)', async () => {
    const onCreate = vi.fn();
    render(
      <RequirementEditDrawer
        open={true}
        editing={null}
        onClose={vi.fn()}
        onCreate={onCreate}
        onUpdate={vi.fn()}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId('req-sources-section')).toBeInTheDocument();
    });
    fireEvent.change(screen.getByLabelText(/编码/), { target: { value: 'REQ-E1' } });
    fireEvent.change(screen.getByLabelText(/标题/), { target: { value: 'X' } });
    fireEvent.change(screen.getByTestId('req-owner-select'), { target: { value: '1' } });
    fireEvent.change(screen.getByTestId('req-expected-date'), {
      target: { value: '2026-09-01' },
    });
    fireEvent.click(screen.getByRole('button', { name: '保存' }));
    await waitFor(() => {
      expect(onCreate).toHaveBeenCalledWith(
        expect.objectContaining({ expectedDate: '2026-09-01' }),
      );
    });
  });
});
