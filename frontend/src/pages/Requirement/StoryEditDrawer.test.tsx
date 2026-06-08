import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { StoryEditDrawer } from './StoryEditDrawer';
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

describe('StoryEditDrawer', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
  });

  /** TC-FES-S03: 新建抽屉默认 owner = 当前登录用户 + Requirement 字段锁定. */
  it('defaults owner to current logged-in user and locks Requirement (TC-FES-S03)', async () => {
    render(
      <StoryEditDrawer
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
      expect(screen.getByTestId('story-owner-select')).toBeInTheDocument();
    });
    await waitFor(() => {
      const sel = screen.getByTestId('story-owner-select') as HTMLSelectElement;
      expect(sel.value).toBe('1');
    });
    // Requirement 字段锁定显示文本而非下拉.
    expect(screen.getByTestId('story-drawer-requirement-display')).toHaveTextContent(
      '登录流程（REQ-42）',
    );
  });

  /** TC-FES-S04: 编辑模式 owner 不 disabled 且可改 → updateStory 收到新 ownerUserId. */
  it('edit mode allows changing owner and calls onUpdate with new ownerUserId (TC-FES-S04)', async () => {
    const onUpdate = vi.fn();
    render(
      <StoryEditDrawer
        open={true}
        requirementId={42}
        requirementCode="REQ-42"
        requirementTitle="登录流程"
        editing={{
          id: 99,
          code: 'STR-99',
          title: 'X',
          status: 'DRAFT',
          priority: 'MEDIUM',
          requirementId: 42,
          ownerUserId: 1,
        }}
        onClose={vi.fn()}
        onCreate={vi.fn()}
        onUpdate={onUpdate}
      />,
    );
    await waitFor(() => {
      const sel = screen.getByTestId('story-owner-select') as HTMLSelectElement;
      expect(sel).not.toBeDisabled();
      expect(sel.value).toBe('1');
    });
    fireEvent.change(screen.getByTestId('story-owner-select'), { target: { value: '2' } });
    fireEvent.click(screen.getByRole('button', { name: '保存' }));
    await waitFor(() => {
      expect(onUpdate).toHaveBeenCalledTimes(1);
    });
    const [id, body] = onUpdate.mock.calls[0];
    expect(id).toBe(99);
    expect(body.ownerUserId).toBe(2);
  });

  /** v0.0.9 form-error parity (Code-M7 family): missing owner shows error instead of silent no-op. */
  it('shows a form error when owner is missing', async () => {
    // Reset auth store so default owner selection picks nothing.
    useAuthStore.setState({ token: 'tk', user: { username: 'nobody' } });
    const onCreate = vi.fn();
    render(
      <StoryEditDrawer
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
      expect(screen.getByTestId('story-owner-select')).toBeInTheDocument();
    });
    fireEvent.change(screen.getByLabelText(/编码/), { target: { value: 'STR-X' } });
    fireEvent.change(screen.getByLabelText(/标题/), { target: { value: 'X' } });
    fireEvent.click(screen.getByRole('button', { name: '保存' }));
    await waitFor(() => {
      expect(screen.getByTestId('story-form-error')).toHaveTextContent('请选择负责人');
    });
    expect(onCreate).not.toHaveBeenCalled();
  });
});
