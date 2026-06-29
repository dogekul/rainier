import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ArchitectDashboardPage } from '../ArchitectDashboardPage';
import { getPendingReviews, getReviewStats } from '../../../api/reviews';

vi.mock('../../../api/reviews', async (orig) => ({
  ...(await orig<typeof import('../../../api/reviews')>()),
  getPendingReviews: vi.fn(),
  getReviewStats: vi.fn(),
}));

function pendingStory(storyId: number, code: string) {
  return {
    kind: 'STORY' as const,
    storyId,
    taskId: null,
    code,
    title: `标题 ${code}`,
    status: 'READY',
    priority: 'HIGH',
    reviewStatus: 'PENDING',
    projectId: 1,
    projectName: 'Apollo',
    sprintId: 1,
    sprintName: 'Sprint 1',
    ownerUserId: 2,
    ownerName: 'Bob',
    ownerLoginName: 'bob',
    createTime: '2026-06-25T00:00:00Z',
  };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <ArchitectDashboardPage />
    </MemoryRouter>,
  );
}

describe('ArchitectDashboardPage (H5 / v0.0.112)', () => {
  beforeEach(() => {
    vi.mocked(getReviewStats).mockReset();
    vi.mocked(getPendingReviews).mockReset();
  });

  /** TC-ARCHUI-001: renders 4 stat tiles with the values from /me/review-stats. */
  it('renders the 4 stat tiles', async () => {
    vi.mocked(getReviewStats).mockResolvedValue({
      pendingStoryCount: 3,
      pendingTaskCount: 5,
      approvedThisWeek: 7,
      rejectedThisWeek: 2,
    });
    vi.mocked(getPendingReviews).mockResolvedValue([pendingStory(1, 'S-1')]);

    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId('architect-stats')).toBeInTheDocument();
    });
    const tiles = screen.getByTestId('architect-stats');
    expect(tiles).toHaveTextContent('3');
    expect(tiles).toHaveTextContent('5');
    expect(tiles).toHaveTextContent('7');
    expect(tiles).toHaveTextContent('2');
    expect(tiles).toHaveTextContent('待我评审 Story');
    expect(tiles).toHaveTextContent('本周通过');
  });

  /** TC-ARCHUI-002: renders Story / Task tabs when there are pending rows. */
  it('renders Story / Task tab buttons', async () => {
    vi.mocked(getReviewStats).mockResolvedValue({
      pendingStoryCount: 1,
      pendingTaskCount: 0,
      approvedThisWeek: 0,
      rejectedThisWeek: 0,
    });
    vi.mocked(getPendingReviews).mockResolvedValue([pendingStory(42, 'S-42')]);

    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId('architect-tab-story')).toBeInTheDocument();
    });
    expect(screen.getByTestId('architect-tab-task')).toBeInTheDocument();
    expect(screen.getByTestId('architect-row-S-42')).toBeInTheDocument();
    expect(screen.getByTestId('architect-go-reviews')).toHaveAttribute('href', '/reviews');
  });

  /** Empty queue → shows empty-state, no tabs. */
  it('shows the empty state when there is no pending review', async () => {
    vi.mocked(getReviewStats).mockResolvedValue({
      pendingStoryCount: 0,
      pendingTaskCount: 0,
      approvedThisWeek: 4,
      rejectedThisWeek: 1,
    });
    vi.mocked(getPendingReviews).mockResolvedValue([]);

    renderPage();

    await waitFor(() => {
      expect(screen.getByTestId('architect-empty')).toBeInTheDocument();
    });
    expect(screen.queryByTestId('architect-tabs')).toBeNull();
  });
});
