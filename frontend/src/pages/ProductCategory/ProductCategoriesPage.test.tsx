import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { ProductCategoriesPage } from './ProductCategoriesPage';
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
          id: 1,
          code: 'CAT-1',
          name: '研发工具',
          status: 'ACTIVE',
          ownerUserId: 1,
          ownerName: 'Alice',
          ownerLoginName: 'alice',
        },
      ],
      total: 1,
      page: 0,
      size: 20,
    }),
  };
});

describe('ProductCategoriesPage', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
  });

  /** TC-FES-PROD-002: /pm/product-categories renders ProductCategoriesPage w/ list + 新建按钮. */
  it('renders product category list w/ 新建产品分类 button (TC-FES-PROD-002)', async () => {
    render(<ProductCategoriesPage />);
    await waitFor(() => {
      expect(screen.getByText('CAT-1')).toBeInTheDocument();
    });
    expect(screen.getByText('研发工具')).toBeInTheDocument();
    expect(screen.getByTestId('product-category-new-btn')).toBeInTheDocument();
  });
});
