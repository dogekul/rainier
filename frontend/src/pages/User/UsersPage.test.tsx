import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { UsersPage } from './UsersPage';

vi.mock('../../api/user', async () => {
  const actual = await vi.importActual<typeof import('../../api/user')>('../../api/user');
  return {
    ...actual,
    listUsers: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 20 }),
    createUser: vi.fn().mockResolvedValue({ id: 99 }),
  };
});

vi.mock('../../api/position', async () => {
  const actual = await vi.importActual<typeof import('../../api/position')>('../../api/position');
  return {
    ...actual,
    listPositions: vi.fn().mockResolvedValue({
      content: [
        { id: 1, code: 'BE_ENG', name: 'Backend Engineer', category: 'TECH', enabled: true },
        { id: 2, code: 'PO', name: 'Product Owner', category: 'PM', enabled: true },
      ],
      total: 2,
      page: 0,
      size: 100,
    }),
  };
});

describe('UsersPage edit drawer position select (TC-FES-H03)', () => {
  it('passes selected positionId on create', async () => {
    render(<UsersPage />);

    // open the new-user drawer
    fireEvent.click(screen.getByTestId('users-new-btn'));

    // wait for positions to load
    await waitFor(() => {
      expect(screen.getByTestId('users-position-select')).toBeInTheDocument();
    });

    // fill required text inputs
    fireEvent.change(screen.getByLabelText(/登录账号/), { target: { value: 'alice' } });
    fireEvent.change(screen.getByLabelText(/姓名/), { target: { value: 'Alice' } });

    // pick a position
    fireEvent.change(screen.getByTestId('users-position-select'), { target: { value: '1' } });

    // click save
    fireEvent.click(screen.getByRole('button', { name: '保存' }));

    const { createUser } = await import('../../api/user');
    await waitFor(() => {
      expect(createUser).toHaveBeenCalledTimes(1);
    });
    const body = (createUser as ReturnType<typeof vi.fn>).mock.calls[0][0];
    expect(body.positionId).toBe(1);
    expect(body.loginName).toBe('alice');
    expect(body.name).toBe('Alice');
  });
});
