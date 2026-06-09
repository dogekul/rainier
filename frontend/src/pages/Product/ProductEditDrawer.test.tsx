import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { ProductEditDrawer } from './ProductEditDrawer';
import { useAuthStore } from '../../store/auth';

vi.mock('../../api/productCategory', async () => {
  const actual = await vi.importActual<typeof import('../../api/productCategory')>(
    '../../api/productCategory',
  );
  return {
    ...actual,
    listProductCategories: vi.fn().mockResolvedValue({
      content: [
        {
          id: 11,
          code: 'CAT-A',
          name: '研发工具',
          status: 'ACTIVE',
          ownerUserId: 1,
        },
        {
          id: 22,
          code: 'CAT-B',
          name: '基础设施',
          status: 'ACTIVE',
          ownerUserId: 1,
        },
      ],
      total: 2,
      page: 0,
      size: 100,
    }),
  };
});

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

  /** TC-FES-PROD-003: Category select 设值正确 — onCreate body.categoryId == 所选 option. */
  it('passes selected categoryId into onCreate payload (TC-FES-PROD-003)', async () => {
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
      const select = screen.getByTestId('product-category-select') as HTMLSelectElement;
      expect(Array.from(select.options).map((o) => o.value)).toContain('22');
    });

    fireEvent.change(screen.getByTestId('product-category-select'), {
      target: { value: '22' },
    });
    expect(
      (screen.getByTestId('product-category-select') as HTMLSelectElement).value,
    ).toBe('22');

    fireEvent.change(screen.getByLabelText('编码 (PROD-...)'), {
      target: { value: 'PROD-X' },
    });
    fireEvent.change(screen.getByLabelText('名称'), { target: { value: '新产品' } });

    fireEvent.click(screen.getByRole('button', { name: '保存' }));

    await waitFor(() => {
      expect(onCreate).toHaveBeenCalledTimes(1);
    });
    const body = onCreate.mock.calls[0][0];
    expect(body.categoryId).toBe(22);
    expect(body.code).toBe('PROD-X');
    expect(body.name).toBe('新产品');
    expect(body.ownerUserId).toBe(1);
  });
});
