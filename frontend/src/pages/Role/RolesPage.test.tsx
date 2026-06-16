import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { RolesPage } from './RolesPage';
import { listRoles } from '../../api/role';

vi.mock('../../api/role', async () => {
  const actual = await vi.importActual<typeof import('../../api/role')>('../../api/role');
  return {
    ...actual,
    listRoles: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 20 }),
    createRole: vi.fn().mockResolvedValue({ id: 99 }),
    updateRole: vi.fn().mockResolvedValue({ id: 7 }),
  };
});

describe('RolesPage', () => {
  beforeEach(() => {
    (listRoles as ReturnType<typeof vi.fn>).mockResolvedValue({
      content: [],
      total: 0,
      page: 0,
      size: 20,
    });
  });
  /** TC-FES-RN-008: ticking 管理员权限 sends adminAccess:true on create. */
  it('sends adminAccess:true when the admin checkbox is ticked on create (TC-FES-RN-008)', async () => {
    const { createRole } = await import('../../api/role');
    (createRole as ReturnType<typeof vi.fn>).mockClear();

    render(<RolesPage />);
    fireEvent.click(screen.getByTestId('roles-new-btn'));

    await waitFor(() => {
      expect(screen.getByTestId('role-admin-access')).toBeInTheDocument();
    });

    fireEvent.change(screen.getByLabelText(/编码/), { target: { value: 'PMO' } });
    fireEvent.change(screen.getByLabelText('名称'), { target: { value: 'PMO' } });
    fireEvent.click(screen.getByTestId('role-admin-access'));

    fireEvent.click(screen.getByRole('button', { name: '保存' }));

    await waitFor(() => {
      expect(createRole).toHaveBeenCalledTimes(1);
    });
    expect(createRole).toHaveBeenCalledWith(expect.objectContaining({ code: 'PMO', adminAccess: true }));
  });

  /** TC-FES-RN-008b: editing a role preloads its adminAccess and sends it back on save. */
  it('preloads and sends adminAccess on the update path (TC-FES-RN-008b)', async () => {
    (listRoles as ReturnType<typeof vi.fn>).mockResolvedValue({
      content: [{ id: 3, code: 'PMO', name: 'PMO', enabled: true, adminAccess: true }],
      total: 1,
      page: 0,
      size: 20,
    });
    const { updateRole } = await import('../../api/role');
    (updateRole as ReturnType<typeof vi.fn>).mockClear();

    render(<RolesPage />);
    await waitFor(() => expect(screen.getByText('编辑')).toBeInTheDocument());
    fireEvent.click(screen.getByText('编辑'));

    // The checkbox reflects the existing adminAccess=true.
    await waitFor(() => {
      expect(screen.getByTestId('role-admin-access')).toBeChecked();
    });

    fireEvent.click(screen.getByRole('button', { name: '保存' }));
    await waitFor(() => {
      expect(updateRole).toHaveBeenCalledTimes(1);
    });
    expect(updateRole).toHaveBeenCalledWith(3, expect.objectContaining({ adminAccess: true }));
  });
});
