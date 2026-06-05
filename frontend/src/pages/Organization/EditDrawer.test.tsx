import { render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { OrganizationEditDrawer } from './EditDrawer';

// Mock the organization API used inside the drawer (it calls getOrganizationTree
// inside useEffect on open). We resolve with an empty tree so the form mounts.
vi.mock('../../api/organization', async () => {
  const actual = await vi.importActual<typeof import('../../api/organization')>(
    '../../api/organization',
  );
  return {
    ...actual,
    getOrganizationTree: vi.fn().mockResolvedValue([]),
  };
});

describe('OrganizationEditDrawer', () => {
  it('renders WITHOUT the PMO checkbox (TC-RMP-FE-001)', async () => {
    render(
      <OrganizationEditDrawer
        open={true}
        editing={null}
        onClose={vi.fn()}
        onSubmit={vi.fn()}
      />,
    );

    // Wait for the tree fetch effect to settle so the drawer is in steady state.
    await waitFor(() => {
      expect(screen.getByText('父节点')).toBeInTheDocument();
    });

    // The PMO checkbox + its label "PMO 团队" must NOT exist anywhere.
    expect(screen.queryByText('PMO 团队')).toBeNull();
    expect(screen.queryByLabelText('PMO 团队')).toBeNull();

    // Surviving labels (sanity baseline) — these MUST still render.
    expect(screen.getByText('父节点')).toBeInTheDocument();
    expect(screen.getByText('类型')).toBeInTheDocument();
    expect(screen.getByText('启用')).toBeInTheDocument();
  });
});
