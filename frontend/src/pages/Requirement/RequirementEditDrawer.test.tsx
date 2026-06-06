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
      ],
      total: 1,
      page: 0,
      size: 200,
    }),
  };
});

describe('RequirementEditDrawer', () => {
  /** TC-FES-D03: 用户勾选 2 个 demand 后保存 → onCreate 收到 body.sourceDemandIds = [10, 20]. */
  it('passes selected demand ids as sourceDemandIds on create (TC-FES-D03)', async () => {
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
  });
});
