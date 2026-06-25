import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { ProductModulesPage } from './ProductModulesPage';
import { useAuthStore } from '../../store/auth';

// 3-level chain M1 (root) → M2 → M3 plus an independent root M4.
vi.mock('../../api/productModule', async () => {
  const actual = await vi.importActual<typeof import('../../api/productModule')>(
    '../../api/productModule',
  );
  return {
    ...actual,
    listProductModules: vi.fn().mockResolvedValue({
      content: [
        {
          id: 1,
          code: 'MOD-M1',
          name: '钱包',
          status: 'ACTIVE',
          productId: 1,
          parentId: null,
          pathName: '钱包',
          ownerUserId: 1,
        },
        {
          id: 2,
          code: 'MOD-M2',
          name: '余额',
          status: 'ACTIVE',
          productId: 1,
          parentId: 1,
          pathName: '钱包 / 余额',
          ownerUserId: 1,
        },
        {
          id: 3,
          code: 'MOD-M3',
          name: '明细',
          status: 'ACTIVE',
          productId: 1,
          parentId: 2,
          pathName: '钱包 / 余额 / 明细',
          ownerUserId: 1,
        },
        {
          id: 4,
          code: 'MOD-M4',
          name: '风控',
          status: 'ACTIVE',
          productId: 1,
          parentId: null,
          pathName: '风控',
          ownerUserId: 1,
        },
      ],
      total: 4,
      page: 0,
      size: 100,
    }),
  };
});

describe('ProductModulesPage', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
  });

  /** TC-FES-PMOD-001: 树形渲染 — M2 嵌套于 M1 内、M3 嵌套于 M2 内、M4 为独立根. */
  it('renders modules as a nested tree with ≥2 levels of containment (TC-FES-PMOD-001)', async () => {
    render(<ProductModulesPage />);
    await waitFor(() => {
      expect(screen.getByTestId('product-module-tree')).toBeInTheDocument();
    });

    const node1 = screen.getByTestId('module-tree-node-1');
    const node2 = screen.getByTestId('module-tree-node-2');
    const node3 = screen.getByTestId('module-tree-node-3');
    const node4 = screen.getByTestId('module-tree-node-4');

    // Nesting: M2 inside M1's subtree, M3 inside M2's subtree.
    expect(node1.contains(node2)).toBe(true);
    expect(node2.contains(node3)).toBe(true);
    // M4 is an independent root — not inside M1.
    expect(node1.contains(node4)).toBe(false);

    // Roots live in the top-level UL.
    const tree = screen.getByTestId('product-module-tree');
    expect(node1.parentElement).toBe(tree);
    expect(node4.parentElement).toBe(tree);

    // Depth indent increases (paddingLeft 8 + depth*24).
    const row1 = node1.querySelector('.module-tree-row') as HTMLElement;
    const row2 = node2.querySelector('.module-tree-row') as HTMLElement;
    const row3 = node3.querySelector('.module-tree-row') as HTMLElement;
    expect(row1.style.paddingLeft).toBe('8px');
    expect(row2.style.paddingLeft).toBe('32px');
    expect(row3.style.paddingLeft).toBe('56px');
  });
});
