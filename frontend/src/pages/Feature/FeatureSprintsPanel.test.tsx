import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import { FeatureSprintsPanel } from './FeatureSprintsPanel';
import { useAuthStore } from '../../store/auth';

vi.mock('../../api/sprintFeature', async () => {
  const actual = await vi.importActual<typeof import('../../api/sprintFeature')>(
    '../../api/sprintFeature',
  );
  return {
    ...actual,
    getFeatureSprints: vi.fn().mockResolvedValue([
      {
        linkId: 1,
        sprintId: 100,
        code: 'SPR-1',
        name: '迭代一',
        status: 'ACTIVE',
        requirementId: 9,
        productId: 5,
      },
    ]),
  };
});

describe('FeatureSprintsPanel', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: 'tk', user: { username: 'alice' } });
  });

  /** TC-FES-SF-003: 查看 feature 的所在迭代. */
  it("shows the feature's iterations (TC-FES-SF-003)", async () => {
    render(<FeatureSprintsPanel featureId={42} />);
    await waitFor(() => {
      expect(screen.getByTestId('feature-sprint-row-100')).toBeInTheDocument();
    });
    expect(screen.getByText('SPR-1')).toBeInTheDocument();
    expect(screen.getByText('迭代一')).toBeInTheDocument();
  });
});
