import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { SprintFeaturePanel } from './SprintFeaturePanel';
import { useAuthStore } from '../../store/auth';
import * as sprintFeatureApi from '../../api/sprintFeature';

vi.mock('../../api/feature', async () => {
  const actual = await vi.importActual<typeof import('../../api/feature')>('../../api/feature');
  return {
    ...actual,
    listFeatures: vi.fn().mockResolvedValue({
      content: [
        { id: 10, code: 'FEAT-A', name: '充值', status: 'ACTIVE', moduleId: 1, ownerUserId: 1 },
        { id: 20, code: 'FEAT-B', name: '提现', status: 'ACTIVE', moduleId: 1, ownerUserId: 1 },
        // moduleId 99 belongs to a DIFFERENT product → must be filtered out of the dropdown.
        { id: 30, code: 'FEAT-X', name: '跨产品', status: 'ACTIVE', moduleId: 99, ownerUserId: 1 },
      ],
      total: 3,
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
      content: [{ id: 1, code: 'MOD-1', name: '钱包', status: 'ACTIVE', productId: 5, ownerUserId: 1 }],
      total: 1,
      page: 0,
      size: 100,
    }),
  };
});

vi.mock('../../api/sprintFeature', async () => {
  const actual = await vi.importActual<typeof import('../../api/sprintFeature')>(
    '../../api/sprintFeature',
  );
  return {
    ...actual,
    getSprintFeatures: vi.fn(),
    createSprintFeature: vi.fn().mockResolvedValue({ id: 99, sprintId: 1, featureId: 10 }),
    deleteSprintFeature: vi.fn().mockResolvedValue(undefined),
  };
});

describe('SprintFeaturePanel', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
    vi.clearAllMocks();
  });

  /** TC-FES-SF-001: 展示已挂功能并支持挂载. */
  it('lists linked features and mounts a new one (TC-FES-SF-001)', async () => {
    const getSprintFeatures = sprintFeatureApi.getSprintFeatures as ReturnType<typeof vi.fn>;
    // First load shows F10 linked; after mount, both F10 and F20.
    getSprintFeatures
      .mockResolvedValueOnce([
        { linkId: 1, featureId: 10, code: 'FEAT-A', name: '充值', status: 'ACTIVE', moduleId: 1 },
      ])
      .mockResolvedValue([
        { linkId: 1, featureId: 10, code: 'FEAT-A', name: '充值', status: 'ACTIVE', moduleId: 1 },
        { linkId: 2, featureId: 20, code: 'FEAT-B', name: '提现', status: 'ACTIVE', moduleId: 1 },
      ]);

    render(<SprintFeaturePanel sprintId={1} productId={5} />);

    await waitFor(() => {
      expect(screen.getByTestId('sprint-feature-row-10')).toBeInTheDocument();
    });

    // Dropdown is product-filtered (only product 5's modules) AND excludes already-linked FEAT-A:
    // → FEAT-B present (id=20), FEAT-A absent (linked), FEAT-X absent (other product, module 99).
    await waitFor(() => {
      const select = screen.getByTestId('sprint-feature-mount-select') as HTMLSelectElement;
      const opts = Array.from(select.options).map((o) => o.value);
      expect(opts).toContain('20');
      expect(opts).not.toContain('10'); // already linked
      expect(opts).not.toContain('30'); // different product
    });

    // Mount FEAT-B (id=20).
    fireEvent.change(screen.getByTestId('sprint-feature-mount-select'), {
      target: { value: '20' },
    });
    fireEvent.click(screen.getByTestId('sprint-feature-mount-btn'));

    await waitFor(() => {
      expect(sprintFeatureApi.createSprintFeature).toHaveBeenCalledWith({
        sprintId: 1,
        featureId: 20,
      });
    });
    await waitFor(() => {
      expect(screen.getByTestId('sprint-feature-row-20')).toBeInTheDocument();
    });
  });

  /** TC-FES-SF-002: 解绑功能. */
  it('unbinds a linked feature (TC-FES-SF-002)', async () => {
    const getSprintFeatures = sprintFeatureApi.getSprintFeatures as ReturnType<typeof vi.fn>;
    getSprintFeatures
      .mockResolvedValueOnce([
        { linkId: 7, featureId: 10, code: 'FEAT-A', name: '充值', status: 'ACTIVE', moduleId: 1 },
      ])
      .mockResolvedValue([]);

    render(<SprintFeaturePanel sprintId={1} productId={5} />);

    await waitFor(() => {
      expect(screen.getByTestId('sprint-feature-row-10')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByTestId('sprint-feature-unbind-10'));

    await waitFor(() => {
      expect(sprintFeatureApi.deleteSprintFeature).toHaveBeenCalledWith(7);
    });
    await waitFor(() => {
      expect(screen.queryByTestId('sprint-feature-row-10')).toBeNull();
    });
  });
});
