import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { ProductModuleEditDrawer } from './ProductModuleEditDrawer';
import { useAuthStore } from '../../store/auth';
import * as productApi from '../../api/product';

vi.mock('../../api/productCategory', async () => {
  const actual = await vi.importActual<typeof import('../../api/productCategory')>(
    '../../api/productCategory',
  );
  return {
    ...actual,
    listProductCategories: vi.fn().mockResolvedValue({
      content: [
        { id: 11, code: 'CAT-A', name: '研发工具', status: 'ACTIVE', ownerUserId: 1 },
        { id: 22, code: 'CAT-B', name: '基础设施', status: 'ACTIVE', ownerUserId: 1 },
      ],
      total: 2,
      page: 0,
      size: 100,
    }),
  };
});

// v0.0.12.1 A2: listProducts called with categoryId — mock filters accordingly.
vi.mock('../../api/product', async () => {
  const actual = await vi.importActual<typeof import('../../api/product')>(
    '../../api/product',
  );
  const ALL_PRODUCTS = [
    {
      id: 100,
      code: 'PROD-A1',
      name: 'Apollo',
      status: 'ACTIVE',
      categoryId: 11,
      ownerUserId: 1,
    },
    {
      id: 101,
      code: 'PROD-A2',
      name: 'Aurora',
      status: 'ACTIVE',
      categoryId: 11,
      ownerUserId: 1,
    },
    {
      id: 200,
      code: 'PROD-B1',
      name: 'Beta',
      status: 'ACTIVE',
      categoryId: 22,
      ownerUserId: 1,
    },
  ];
  return {
    ...actual,
    listProducts: vi.fn().mockImplementation((params: { categoryId?: number } = {}) => {
      const filtered = params.categoryId
        ? ALL_PRODUCTS.filter((p) => p.categoryId === params.categoryId)
        : ALL_PRODUCTS;
      return Promise.resolve({ content: filtered, total: filtered.length, page: 0, size: 100 });
    }),
    getProduct: vi.fn(),
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
   * TC-FES-PMOD-001 (v0.0.12.1 A2): Category 切换 → listProducts 服务器侧 categoryId 过滤 + Product 清空.
   */
  it('fetches Products server-filtered by categoryId when Category changes (TC-FES-PMOD-001)', async () => {
    render(
      <ProductModuleEditDrawer
        open={true}
        editing={null}
        onClose={vi.fn()}
        onCreate={vi.fn()}
        onUpdate={vi.fn()}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId('product-module-category-select')).toBeInTheDocument();
    });

    // Initial open: listProducts called once with no categoryId (size only).
    await waitFor(() => {
      expect(productApi.listProducts).toHaveBeenCalledWith({ size: 100 });
    });

    // Pick Category 11 → listProducts re-called with categoryId=11.
    fireEvent.change(screen.getByTestId('product-module-category-select'), {
      target: { value: '11' },
    });
    await waitFor(() => {
      expect(productApi.listProducts).toHaveBeenLastCalledWith({
        categoryId: 11,
        size: 100,
      });
    });
    await waitFor(() => {
      const productSelect = screen.getByTestId(
        'product-module-product-select',
      ) as HTMLSelectElement;
      const options = Array.from(productSelect.options).map((o) => o.value);
      expect(options).toContain('100');
      expect(options).toContain('101');
      expect(options).not.toContain('200');
    });

    // Pick Product 100.
    fireEvent.change(screen.getByTestId('product-module-product-select'), {
      target: { value: '100' },
    });
    expect(
      (screen.getByTestId('product-module-product-select') as HTMLSelectElement).value,
    ).toBe('100');

    // Switch Category 11 → 22: Product cleared, listProducts re-fetched with categoryId=22.
    fireEvent.change(screen.getByTestId('product-module-category-select'), {
      target: { value: '22' },
    });
    await waitFor(() => {
      expect(
        (screen.getByTestId('product-module-product-select') as HTMLSelectElement).value,
      ).toBe('');
    });
    await waitFor(() => {
      expect(productApi.listProducts).toHaveBeenLastCalledWith({
        categoryId: 22,
        size: 100,
      });
    });
  });
});
