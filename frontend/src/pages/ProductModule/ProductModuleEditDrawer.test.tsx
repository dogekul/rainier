import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { ProductModuleEditDrawer } from './ProductModuleEditDrawer';
import { useAuthStore } from '../../store/auth';
import * as productModuleApi from '../../api/productModule';

vi.mock('../../api/product', async () => {
  const actual = await vi.importActual<typeof import('../../api/product')>(
    '../../api/product',
  );
  return {
    ...actual,
    listProducts: vi.fn().mockResolvedValue({
      content: [
        { id: 1, code: 'PROD-A', name: 'Apollo', status: 'ACTIVE', ownerUserId: 1 },
        { id: 2, code: 'PROD-B', name: 'Beta', status: 'ACTIVE', ownerUserId: 1 },
      ],
      total: 2,
      page: 0,
      size: 100,
    }),
  };
});

// v0.0.13: parent candidates fetched server-filtered by productId (A2 pattern).
vi.mock('../../api/productModule', async () => {
  const actual = await vi.importActual<typeof import('../../api/productModule')>(
    '../../api/productModule',
  );
  const ALL_MODULES = [
    {
      id: 10,
      code: 'MOD-A1',
      name: '钱包',
      pathName: '钱包',
      status: 'ACTIVE',
      productId: 1,
      parentId: null,
      ownerUserId: 1,
    },
    {
      id: 20,
      code: 'MOD-A2',
      name: '余额',
      pathName: '钱包 / 余额',
      status: 'ACTIVE',
      productId: 1,
      parentId: 10,
      ownerUserId: 1,
    },
    {
      id: 30,
      code: 'MOD-B1',
      name: '风控',
      pathName: '风控',
      status: 'ACTIVE',
      productId: 2,
      parentId: null,
      ownerUserId: 1,
    },
  ];
  return {
    ...actual,
    listProductModules: vi.fn().mockImplementation((params: { productId?: number } = {}) => {
      const filtered = params.productId
        ? ALL_MODULES.filter((m) => m.productId === params.productId)
        : ALL_MODULES;
      return Promise.resolve({ content: filtered, total: filtered.length, page: 0, size: 100 });
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

describe('ProductModuleEditDrawer', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
    vi.clearAllMocks();
  });

  /**
   * TC-FES-PMOD-002 (v0.0.13): Product 切换 → parentModule 候选服务器侧 productId 过滤 + 清空；
   * 留空 parent → onCreate 不带 parentId（顶层）。
   */
  it('refreshes parent candidates per product and posts top-level without parentId', async () => {
    const onCreate = vi.fn();
    render(
      <ProductModuleEditDrawer
        open={true}
        editing={null}
        onClose={vi.fn()}
        onCreate={onCreate}
        onUpdate={vi.fn()}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId('product-module-product-select')).toBeInTheDocument();
    });

    // No product picked yet → no parent fetch.
    expect(productModuleApi.listProductModules).not.toHaveBeenCalled();

    // Pick Product A → parent candidates fetched with productId=1.
    fireEvent.change(screen.getByTestId('product-module-product-select'), {
      target: { value: '1' },
    });
    await waitFor(() => {
      expect(productModuleApi.listProductModules).toHaveBeenCalledWith({
        productId: 1,
        size: 100,
      });
    });
    await waitFor(() => {
      const parentSelect = screen.getByTestId(
        'product-module-parent-select',
      ) as HTMLSelectElement;
      const options = Array.from(parentSelect.options).map((o) => o.value);
      expect(options).toContain('10');
      expect(options).toContain('20');
      expect(options).not.toContain('30');
    });

    // Parent option labels render pathName ("钱包 / 余额"), not just name.
    const parentSelect = screen.getByTestId('product-module-parent-select') as HTMLSelectElement;
    const labels = Array.from(parentSelect.options).map((o) => o.textContent ?? '');
    expect(labels.some((l) => l.includes('钱包 / 余额'))).toBe(true);

    // Pick parent 10, then switch Product A → B: parent cleared + refetched for B.
    fireEvent.change(parentSelect, { target: { value: '10' } });
    expect(parentSelect.value).toBe('10');
    fireEvent.change(screen.getByTestId('product-module-product-select'), {
      target: { value: '2' },
    });
    await waitFor(() => {
      expect(
        (screen.getByTestId('product-module-parent-select') as HTMLSelectElement).value,
      ).toBe('');
    });
    await waitFor(() => {
      expect(productModuleApi.listProductModules).toHaveBeenLastCalledWith({
        productId: 2,
        size: 100,
      });
    });

    // Leave parent empty → onCreate without parentId (top-level).
    fireEvent.change(screen.getByLabelText('编码 (MOD-...)'), {
      target: { value: 'MOD-NEW' },
    });
    fireEvent.change(screen.getByLabelText('名称'), { target: { value: '新模块' } });
    fireEvent.click(screen.getByRole('button', { name: '保存' }));
    await waitFor(() => {
      expect(onCreate).toHaveBeenCalledTimes(1);
    });
    const body = onCreate.mock.calls[0][0];
    expect(body.productId).toBe(2);
    expect(body.parentId).toBeUndefined();
  });
});
