import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { ProductEditDrawer } from './ProductEditDrawer';
import { useAuthStore } from '../../store/auth';

vi.mock('../../api/user', async () => {
  const actual = await vi.importActual<typeof import('../../api/user')>('../../api/user');
  return {
    ...actual,
    listUsers: vi.fn().mockResolvedValue({
      content: [{ id: 1, loginName: 'alice', name: 'Alice', isInternal: true, enabled: true }],
      total: 1,
      page: 0,
      size: 100,
    }),
  };
});

describe('ProductEditDrawer', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
  });

  /** v0.0.13: Category select 已随 ProductCategory 删除 — onCreate payload 不含 categoryId. */
  it('passes payload without categoryId into onCreate (v0.0.13)', async () => {
    const onCreate = vi.fn();
    render(
      <ProductEditDrawer
        open={true}
        editing={null}
        onClose={vi.fn()}
        onCreate={onCreate}
        onUpdate={vi.fn()}
      />,
    );

    await waitFor(() => {
      const owner = screen.getByTestId('product-owner-select') as HTMLSelectElement;
      expect(owner.value).toBe('1'); // defaulted to current user
    });

    // The Category select must be gone entirely.
    expect(screen.queryByTestId('product-category-select')).toBeNull();

    fireEvent.change(screen.getByLabelText('编码 (PROD-...)'), {
      target: { value: 'PROD-X' },
    });
    fireEvent.change(screen.getByLabelText('名称'), { target: { value: '新产品' } });

    fireEvent.click(screen.getByRole('button', { name: '保存' }));

    await waitFor(() => {
      expect(onCreate).toHaveBeenCalledTimes(1);
    });
    const body = onCreate.mock.calls[0][0];
    expect(body.code).toBe('PROD-X');
    expect(body.name).toBe('新产品');
    expect(body.ownerUserId).toBe(1);
    expect('categoryId' in body).toBe(false);
  });
});
