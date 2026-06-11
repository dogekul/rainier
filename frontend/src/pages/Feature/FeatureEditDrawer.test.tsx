import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { FeatureEditDrawer } from './FeatureEditDrawer';
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
        {
          id: 1,
          code: 'PROD-A',
          name: 'Apollo',
          status: 'ACTIVE',
          ownerUserId: 1,
        },
        {
          id: 2,
          code: 'PROD-B',
          name: 'Beta',
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

// v0.0.12.1 A2: listProductModules now called with productId — mock returns filtered results.
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
      name: 'Module B1',
      pathName: 'Module B1',
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
    getProductModule: vi.fn(),
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

describe('FeatureEditDrawer', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
    vi.clearAllMocks();
  });

  /**
   * TC-FES-PROD-004 (v0.0.12.1 A2 — server-side filter migration):
   * Product 切换 → listProductModules 服务器侧 productId 过滤 + 已选 Module 清空.
   */
  it(
    'fetches Modules server-filtered by productId when Product changes (TC-FES-PROD-004)',
    async () => {
      render(
        <FeatureEditDrawer
          open={true}
          editing={null}
          onClose={vi.fn()}
          onCreate={vi.fn()}
          onUpdate={vi.fn()}
        />,
      );
      await waitFor(() => {
        expect(screen.getByTestId('feature-product-select')).toBeInTheDocument();
      });

      // No Product picked yet → no listProductModules call.
      expect(productModuleApi.listProductModules).not.toHaveBeenCalled();

      // Pick Product 1 → listProductModules called with productId=1.
      fireEvent.change(screen.getByTestId('feature-product-select'), { target: { value: '1' } });
      await waitFor(() => {
        expect(productModuleApi.listProductModules).toHaveBeenCalledWith({
          productId: 1,
          size: 100,
        });
      });
      await waitFor(() => {
        const moduleSelect = screen.getByTestId('feature-module-select') as HTMLSelectElement;
        const options = Array.from(moduleSelect.options).map((o) => o.value);
        expect(options).toContain('10');
        expect(options).toContain('20');
        expect(options).not.toContain('30');
      });

      // Pick Module 10.
      fireEvent.change(screen.getByTestId('feature-module-select'), { target: { value: '10' } });
      expect((screen.getByTestId('feature-module-select') as HTMLSelectElement).value).toBe('10');

      // Switch Product 1 → 2: Module cleared, listProductModules re-fetched with productId=2.
      fireEvent.change(screen.getByTestId('feature-product-select'), { target: { value: '2' } });
      await waitFor(() => {
        expect((screen.getByTestId('feature-module-select') as HTMLSelectElement).value).toBe('');
      });
      await waitFor(() => {
        expect(productModuleApi.listProductModules).toHaveBeenLastCalledWith({
          productId: 2,
          size: 100,
        });
      });
      const optionsAfter = Array.from(
        (screen.getByTestId('feature-module-select') as HTMLSelectElement).options,
      ).map((o) => o.value);
      expect(optionsAfter).toContain('30');
      expect(optionsAfter).not.toContain('10');
      expect(optionsAfter).not.toContain('20');
    },
  );

  /** TC-FES-FEAT-001 (v0.0.13): 模块下拉选项文本使用 pathName（多层显示 "钱包 / 余额"）. */
  it('renders module options with pathName labels (TC-FES-FEAT-001)', async () => {
    render(
      <FeatureEditDrawer
        open={true}
        editing={null}
        onClose={vi.fn()}
        onCreate={vi.fn()}
        onUpdate={vi.fn()}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId('feature-product-select')).toBeInTheDocument();
    });
    fireEvent.change(screen.getByTestId('feature-product-select'), { target: { value: '1' } });
    await waitFor(() => {
      const moduleSelect = screen.getByTestId('feature-module-select') as HTMLSelectElement;
      expect(moduleSelect.options.length).toBe(3); // placeholder + 2 modules
    });
    const labels = Array.from(
      (screen.getByTestId('feature-module-select') as HTMLSelectElement).options,
    ).map((o) => o.textContent ?? '');
    expect(labels.some((l) => l.includes('钱包（MOD-A1）'))).toBe(true);
    expect(labels.some((l) => l.includes('钱包 / 余额（MOD-A2）'))).toBe(true);
  });
});
