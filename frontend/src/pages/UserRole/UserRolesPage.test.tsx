import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { UserRolesPage } from './UserRolesPage';

vi.mock('../../api/userRole', async () => {
  const actual = await vi.importActual<typeof import('../../api/userRole')>(
    '../../api/userRole',
  );
  return {
    ...actual,
    listUserRoles: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 20 }),
    createUserRole: vi.fn().mockResolvedValue({ id: 99 }),
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
      size: 100,
    }),
  };
});

vi.mock('../../api/role', async () => {
  const actual = await vi.importActual<typeof import('../../api/role')>('../../api/role');
  return {
    ...actual,
    listRoles: vi.fn().mockResolvedValue({
      content: [{ id: 11, code: 'PMO', name: 'PMO', enabled: true }],
      total: 1,
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
      ],
      total: 1,
      page: 0,
      size: 100,
    }),
  };
});

describe('UserRolesPage', () => {
  /** TC-FES-P05: blank project select → createUserRole receives projectId: null (not undefined / not 0). */
  it('blank project select sends projectId=null (TC-FES-P05)', async () => {
    render(<UserRolesPage />);
    fireEvent.click(screen.getByTestId('user-roles-new-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('user-roles-project-select')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByTestId('user-roles-user-select'), {
      target: { value: '1' },
    });
    fireEvent.change(screen.getByTestId('user-roles-role-select'), {
      target: { value: '11' },
    });
    // Leave project select blank — the default value.

    fireEvent.click(screen.getByRole('button', { name: '保存' }));

    const { createUserRole } = await import('../../api/userRole');
    await waitFor(() => {
      expect(createUserRole).toHaveBeenCalledTimes(1);
    });
    const body = (createUserRole as ReturnType<typeof vi.fn>).mock.calls[0][0];
    expect(body.userId).toBe(1);
    expect(body.roleId).toBe(11);
    expect(body.projectId).toBeNull();
  });
});
