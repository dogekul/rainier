import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ReviewsPage } from './ReviewsPage';
import { getPendingReviews, submitReview } from '../../api/reviews';

function review(storyId: number, code: string, priority = 'MEDIUM') {
  return {
    storyId,
    code,
    title: `标题 ${code}`,
    status: 'READY',
    priority,
    reviewStatus: 'PENDING',
    projectId: 1,
    projectName: 'Apollo',
    sprintId: 1,
    sprintName: 'Sprint 1',
    ownerUserId: 2,
    ownerName: 'Bob',
    ownerLoginName: 'bob',
    createTime: '2026-06-18T00:00:00Z',
  };
}

vi.mock('../../api/reviews', async (orig) => ({
  ...(await orig<typeof import('../../api/reviews')>()),
  getPendingReviews: vi.fn(),
  submitReview: vi.fn(() => Promise.resolve()),
}));

function renderPage() {
  return render(
    <MemoryRouter>
      <ReviewsPage />
    </MemoryRouter>,
  );
}

describe('ReviewsPage', () => {
  beforeEach(() => {
    vi.mocked(submitReview).mockClear();
    vi.mocked(getPendingReviews).mockReset();
  });

  /** TC-REVP-01: renders pending stories + count. */
  it('renders the pending review list with a count (TC-REVP-01)', async () => {
    vi.mocked(getPendingReviews).mockResolvedValue([review(10, 'S-10'), review(11, 'S-11')]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('reviews-row-10')).toBeInTheDocument());
    expect(screen.getByTestId('reviews-row-11')).toBeInTheDocument();
    expect(screen.getByTestId('reviews-summary')).toHaveTextContent('2');
  });

  /** TC-REVP-02: clicking 通过 calls submitReview(id, APPROVED) and refetches (row leaves). */
  it('approves a story and refetches (TC-REVP-02)', async () => {
    vi.mocked(getPendingReviews)
      .mockResolvedValueOnce([review(10, 'S-10')])
      .mockResolvedValueOnce([]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('reviews-row-10')).toBeInTheDocument());

    fireEvent.click(screen.getByTestId('reviews-approve-10'));

    await waitFor(() => expect(submitReview).toHaveBeenCalledWith(10, 'APPROVED'));
    await waitFor(() => expect(screen.getByTestId('reviews-empty')).toBeInTheDocument());
    expect(getPendingReviews).toHaveBeenCalledTimes(2);
  });

  /** TC-REVP-03: empty queue → friendly empty state. */
  it('shows an empty state when there is nothing to review (TC-REVP-03)', async () => {
    vi.mocked(getPendingReviews).mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('reviews-empty')).toBeInTheDocument());
  });
});
