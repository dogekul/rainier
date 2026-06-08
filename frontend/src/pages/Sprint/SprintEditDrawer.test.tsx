import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { SprintEditDrawer } from './SprintEditDrawer';
import { useAuthStore } from '../../store/auth';

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

describe('SprintEditDrawer', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
  });

  /** TC-FES-SPR-03: 新建抽屉默认 owner = 当前登录用户 + Requirement 字段锁定. */
  it('defaults owner to current logged-in user and locks Requirement (TC-FES-SPR-03)', async () => {
    render(
      <SprintEditDrawer
        open={true}
        requirementId={42}
        requirementCode="REQ-42"
        requirementTitle="登录流程"
        editing={null}
        onClose={vi.fn()}
        onCreate={vi.fn()}
        onUpdate={vi.fn()}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId('sprint-owner-select')).toBeInTheDocument();
    });
    await waitFor(() => {
      const sel = screen.getByTestId('sprint-owner-select') as HTMLSelectElement;
      expect(sel.value).toBe('1');
    });
    expect(screen.getByTestId('sprint-drawer-requirement-display')).toHaveTextContent(
      '登录流程（REQ-42）',
    );
  });

  /** TC-FES-SPR-04: 编辑抽屉 owner 可改 → updateSprint 收到新 ownerUserId. */
  it('edit mode allows changing owner and calls onUpdate with new ownerUserId (TC-FES-SPR-04)', async () => {
    const onUpdate = vi.fn();
    render(
      <SprintEditDrawer
        open={true}
        requirementId={42}
        requirementCode="REQ-42"
        requirementTitle="登录流程"
        editing={{
          id: 99,
          code: 'SPR-99',
          name: 'Phase 1',
          status: 'PLANNING',
          requirementId: 42,
          ownerUserId: 1,
        }}
        onClose={vi.fn()}
        onCreate={vi.fn()}
        onUpdate={onUpdate}
      />,
    );
    await waitFor(() => {
      const sel = screen.getByTestId('sprint-owner-select') as HTMLSelectElement;
      expect(sel).not.toBeDisabled();
      expect(sel.value).toBe('1');
    });
    fireEvent.change(screen.getByTestId('sprint-owner-select'), { target: { value: '2' } });
    fireEvent.click(screen.getByRole('button', { name: '保存' }));
    await waitFor(() => {
      expect(onUpdate).toHaveBeenCalledTimes(1);
    });
    const [id, body] = onUpdate.mock.calls[0];
    expect(id).toBe(99);
    expect(body.ownerUserId).toBe(2);
  });

  /** Form-error parity with v0.0.8.1 Code-M7 pattern. */
  it('shows a form error when owner is missing', async () => {
    useAuthStore.setState({ token: 'tk', user: { username: 'nobody' } });
    const onCreate = vi.fn();
    render(
      <SprintEditDrawer
        open={true}
        requirementId={42}
        requirementCode="REQ-42"
        requirementTitle="登录流程"
        editing={null}
        onClose={vi.fn()}
        onCreate={onCreate}
        onUpdate={vi.fn()}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId('sprint-owner-select')).toBeInTheDocument();
    });
    fireEvent.change(screen.getByLabelText(/编码/), { target: { value: 'SPR-X' } });
    fireEvent.change(screen.getByLabelText(/名称/), { target: { value: 'Phase 1' } });
    fireEvent.click(screen.getByRole('button', { name: '保存' }));
    await waitFor(() => {
      expect(screen.getByTestId('sprint-form-error')).toHaveTextContent('请选择负责人');
    });
    expect(onCreate).not.toHaveBeenCalled();
  });
});
