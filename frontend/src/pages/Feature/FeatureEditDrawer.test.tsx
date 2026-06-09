import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { FeatureEditDrawer } from './FeatureEditDrawer';
import { useAuthStore } from '../../store/auth';

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
          categoryId: 1,
          ownerUserId: 1,
        },
        {
          id: 2,
          code: 'PROD-B',
          name: 'Beta',
          status: 'ACTIVE',
          categoryId: 2,
          ownerUserId: 1,
        },
      ],
      total: 2,
      page: 0,
      size: 100,
    }),
  };
});

vi.mock('../../api/productModule', async () => {
  const actual = await vi.importActual<typeof import('../../api/productModule')>(
    '../../api/productModule',
  );
  return {
    ...actual,
    listProductModules: vi.fn().mockResolvedValue({
      content: [
        {
          id: 10,
          code: 'MOD-A1',
          name: 'Module A1',
          status: 'ACTIVE',
          productId: 1,
          ownerUserId: 1,
        },
        {
          id: 20,
          code: 'MOD-A2',
          name: 'Module A2',
          status: 'ACTIVE',
          productId: 1,
          ownerUserId: 1,
        },
        {
          id: 30,
          code: 'MOD-B1',
          name: 'Module B1',
          status: 'ACTIVE',
          productId: 2,
          ownerUserId: 1,
        },
      ],
      total: 3,
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

describe('FeatureEditDrawer', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
  });

  /** TC-FES-PROD-004: Product 切换后 Module 选项过滤 + 已选 Module 清空. */
  it('filters Module options by Product and clears Module on Product change (TC-FES-PROD-004)', async () => {
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

    // Pick Product 1 → only id=10 and id=20 in Module dropdown.
    fireEvent.change(screen.getByTestId('feature-product-select'), { target: { value: '1' } });
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

    // Switch Product 1 → 2, expect Module cleared + only id=30 visible.
    fireEvent.change(screen.getByTestId('feature-product-select'), { target: { value: '2' } });
    await waitFor(() => {
      expect((screen.getByTestId('feature-module-select') as HTMLSelectElement).value).toBe('');
    });
    const optionsAfter = Array.from(
      (screen.getByTestId('feature-module-select') as HTMLSelectElement).options,
    ).map((o) => o.value);
    expect(optionsAfter).toContain('30');
    expect(optionsAfter).not.toContain('10');
    expect(optionsAfter).not.toContain('20');
  });
});
