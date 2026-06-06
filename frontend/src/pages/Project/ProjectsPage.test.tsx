import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { ProjectsPage } from './ProjectsPage';
import { useAuthStore } from '../../store/auth';

vi.mock('../../api/project', async () => {
  const actual = await vi.importActual<typeof import('../../api/project')>('../../api/project');
  return {
    ...actual,
    listProjects: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 20 }),
    createProject: vi.fn().mockResolvedValue({ id: 99 }),
    updateProject: vi.fn().mockResolvedValue({ id: 7 }),
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

describe('ProjectsPage', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
  });

  /** TC-FES-P03: 新建抽屉默认 owner = 当前登录用户. */
  it('defaults owner select to the current logged-in user (TC-FES-P03)', async () => {
    render(<ProjectsPage />);
    fireEvent.click(screen.getByTestId('projects-new-btn'));
    await waitFor(() => {
      expect(screen.getByTestId('projects-owner-select')).toBeInTheDocument();
    });
    await waitFor(() => {
      const sel = screen.getByTestId('projects-owner-select') as HTMLSelectElement;
      expect(sel.value).toBe('1');
    });
  });

  /** v0.0.8.1 Code-M7: 缺 owner 时点保存 SHALL 显示表单错误而不是静默 no-op. */
  it('shows a form error instead of silently no-op when owner is missing (Code-M7)', async () => {
    // Start with no logged-in user so listUsers' default-selection doesn't pick anyone.
    act(() => {
      useAuthStore.setState({ token: 'tk', user: { username: 'nobody' } });
    });
    const { createProject } = await import('../../api/project');
    (createProject as ReturnType<typeof vi.fn>).mockClear();

    render(<ProjectsPage />);
    fireEvent.click(screen.getByTestId('projects-new-btn'));
    await waitFor(() => {
      expect(screen.getByTestId('projects-owner-select')).toBeInTheDocument();
    });
    // Owner select remains '' because no user matches "nobody" loginName.
    const sel = screen.getByTestId('projects-owner-select') as HTMLSelectElement;
    expect(sel.value).toBe('');
    // Fill required text fields so only owner is missing.
    fireEvent.change(screen.getByLabelText(/编码/), { target: { value: 'PROJ-X' } });
    fireEvent.change(screen.getByLabelText(/^名称/), { target: { value: 'X' } });
    fireEvent.click(screen.getByRole('button', { name: '保存' }));
    await waitFor(() => {
      expect(screen.getByTestId('projects-form-error')).toHaveTextContent('请选择负责人');
    });
    expect(createProject).not.toHaveBeenCalled();
  });

  /** TC-FES-P04: 编辑抽屉 owner 不 disabled 且可改，updateProject 收到新 ownerUserId. */
  it('edit drawer allows changing owner (TC-FES-P04)', async () => {
    const { listProjects, updateProject } = await import('../../api/project');
    (listProjects as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      content: [
        {
          id: 7,
          code: 'PROJ-001',
          name: 'X',
          description: null,
          status: 'PLANNING',
          ownerUserId: 1,
          ownerName: 'Alice',
          ownerLoginName: 'alice',
          startDate: null,
          endDate: null,
          enabled: true,
        },
      ],
      total: 1,
      page: 0,
      size: 20,
    });
    render(<ProjectsPage />);
    await waitFor(() => {
      expect(screen.getByText('PROJ-001')).toBeInTheDocument();
    });
    fireEvent.click(screen.getByRole('button', { name: '编辑' }));
    await waitFor(() => {
      const sel = screen.getByTestId('projects-owner-select') as HTMLSelectElement;
      expect(sel).not.toBeDisabled();
      expect(sel.value).toBe('1');
    });
    fireEvent.change(screen.getByTestId('projects-owner-select'), {
      target: { value: '2' },
    });
    fireEvent.click(screen.getByRole('button', { name: '保存' }));
    await waitFor(() => {
      expect(updateProject).toHaveBeenCalledTimes(1);
    });
    const args = (updateProject as ReturnType<typeof vi.fn>).mock.calls[0];
    expect(args[0]).toBe(7);
    expect(args[1].ownerUserId).toBe(2);
  });
});
