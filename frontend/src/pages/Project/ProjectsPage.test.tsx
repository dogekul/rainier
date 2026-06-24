import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
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

vi.mock('../../api/milestone', async () => {
  const actual =
    await vi.importActual<typeof import('../../api/milestone')>('../../api/milestone');
  return {
    ...actual,
    listMilestones: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 }),
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
    // Fill required text field so only owner is missing (编号自动生成，无输入).
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
          projectType: 'CASUAL',
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

  /** TC-FES-PROJTYPE-001: 新建抽屉含项目类型下拉且默认轻量(CASUAL). */
  it('new drawer has a project-type select defaulting to 轻量 (TC-FES-PROJTYPE-001)', async () => {
    render(<ProjectsPage />);
    fireEvent.click(screen.getByTestId('projects-new-btn'));
    await waitFor(() => {
      expect(screen.getByTestId('projects-type-select')).toBeInTheDocument();
    });
    const typeSelect = screen.getByTestId('projects-type-select') as HTMLSelectElement;
    expect(typeSelect.value).toBe('CASUAL');
    // v0.0.48: 4 options — 轻量 + 主业-功能建设 / 主业-技术改造 / 对外-交付
    expect(within(typeSelect).getByText('轻量')).toBeInTheDocument();
    expect(within(typeSelect).getByText('主业-功能建设')).toBeInTheDocument();
    expect(within(typeSelect).getByText('主业-技术改造')).toBeInTheDocument();
    expect(within(typeSelect).getByText('对外-交付')).toBeInTheDocument();
  });

  /** TC-FES-PROJTYPE-002: 表格类型列显示中文. */
  it('renders the type column with a Chinese label (TC-FES-PROJTYPE-002)', async () => {
    const { listProjects } = await import('../../api/project');
    (listProjects as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      content: [
        {
          id: 11,
          code: 'PROJ-CORE',
          name: '主业功能',
          description: null,
          status: 'ACTIVE',
          projectType: 'CORE_FEATURE',
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
      expect(screen.getByText('PROJ-CORE')).toBeInTheDocument();
    });
    // Column header present.
    expect(screen.getByText('类型')).toBeInTheDocument();
    // The row's type cell shows the 4-option Chinese label.
    const row = screen.getByText('PROJ-CORE').closest('tr') as HTMLElement;
    expect(within(row).getByText('主业-功能建设')).toBeInTheDocument();
  });

  /** TC-FES-PROJTYPE-003: 类型过滤触发带 projectType 的查询. */
  it('type filter triggers a query carrying projectType (TC-FES-PROJTYPE-003)', async () => {
    const { listProjects } = await import('../../api/project');
    render(<ProjectsPage />);
    // Let the initial mount fetch settle, then attribute the next call to the filter change.
    await waitFor(() => {
      expect(listProjects).toHaveBeenCalled();
    });
    (listProjects as ReturnType<typeof vi.fn>).mockClear();
    fireEvent.change(screen.getByTestId('projects-type-filter'), {
      target: { value: 'EXTERNAL_DELIVERY' },
    });
    await waitFor(() => {
      expect(listProjects).toHaveBeenCalledWith(
        expect.objectContaining({ projectType: 'EXTERNAL_DELIVERY' }),
      );
    });
  });

  /** TC-FES-PROJTYPE-004: 提交携带 projectType. */
  it('submits createProject carrying the chosen projectType (TC-FES-PROJTYPE-004)', async () => {
    const { createProject } = await import('../../api/project');
    (createProject as ReturnType<typeof vi.fn>).mockClear();
    render(<ProjectsPage />);
    fireEvent.click(screen.getByTestId('projects-new-btn'));
    await waitFor(() => {
      expect(screen.getByTestId('projects-type-select')).toBeInTheDocument();
    });
    // Owner auto-defaults to the logged-in user (alice → id 1); 编号自动生成，仅填名称+类型.
    fireEvent.change(screen.getByLabelText(/^名称/), { target: { value: 'X' } });
    fireEvent.change(screen.getByTestId('projects-type-select'), {
      target: { value: 'CORE_TECH' },
    });
    fireEvent.click(screen.getByRole('button', { name: '保存' }));
    await waitFor(() => {
      expect(createProject).toHaveBeenCalledWith(
        expect.objectContaining({ projectType: 'CORE_TECH' }),
      );
    });
    // v0.0.49: 创建不再发送 code（自动生成）
    expect(createProject).not.toHaveBeenCalledWith(expect.objectContaining({ code: expect.anything() }));
  });

  /** TC-FES-MILE-001: 里程碑按钮展开内联面板 + listMilestones(projectId). */
  it('expands the milestones panel on 里程碑 click (TC-FES-MILE-001)', async () => {
    const { listProjects } = await import('../../api/project');
    const { listMilestones } = await import('../../api/milestone');
    (listProjects as ReturnType<typeof vi.fn>).mockResolvedValueOnce({
      content: [
        {
          id: 7,
          code: 'PROJ-001',
          name: 'X',
          description: null,
          status: 'PLANNING',
          projectType: 'CASUAL',
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
    fireEvent.click(screen.getByTestId('projects-milestones-btn-7'));
    await waitFor(() => {
      expect(screen.getByTestId('milestones-panel-7')).toBeInTheDocument();
    });
    expect(listMilestones).toHaveBeenCalledWith(expect.objectContaining({ projectId: 7 }));
  });
});
