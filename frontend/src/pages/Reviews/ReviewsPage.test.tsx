import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ReviewsPage } from './ReviewsPage';
import {
  getPendingReviews,
  submitReview,
  submitTaskReview,
} from '../../api/reviews';

function storyReview(storyId: number, code: string, priority = 'MEDIUM') {
  return {
    kind: 'STORY' as const,
    storyId,
    taskId: null,
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

function taskReview(taskId: number, code: string, priority = 'MEDIUM') {
  return {
    kind: 'TASK' as const,
    storyId: null,
    taskId,
    code,
    title: `任务 ${code}`,
    status: 'TODO',
    priority,
    reviewStatus: 'PENDING',
    projectId: 1,
    projectName: 'Apollo',
    sprintId: null,
    sprintName: null,
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
  submitTaskReview: vi.fn(() => Promise.resolve()),
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
    vi.mocked(submitTaskReview).mockClear();
    vi.mocked(getPendingReviews).mockReset();
  });

  /** TC-REVP-01: renders pending stories + count. */
  it('renders the pending review list with a count (TC-REVP-01)', async () => {
    vi.mocked(getPendingReviews).mockResolvedValue([
      storyReview(10, 'S-10'),
      storyReview(11, 'S-11'),
    ]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('reviews-row-S-10')).toBeInTheDocument());
    expect(screen.getByTestId('reviews-row-S-11')).toBeInTheDocument();
    expect(screen.getByTestId('reviews-summary')).toHaveTextContent('2');
  });

  /** TC-REVP-02: clicking 通过 calls submitReview(id, APPROVED) and refetches (row leaves). */
  it('approves a story and refetches (TC-REVP-02)', async () => {
    vi.mocked(getPendingReviews)
      .mockResolvedValueOnce([storyReview(10, 'S-10')])
      .mockResolvedValueOnce([]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('reviews-row-S-10')).toBeInTheDocument());

    fireEvent.click(screen.getByTestId('reviews-approve-S-10'));

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

  /** TC-TREV-007: Task tab — approve calls submitTaskReview. */
  it('approves a task from the Task tab (TC-TREV-007)', async () => {
    vi.mocked(getPendingReviews)
      .mockResolvedValueOnce([taskReview(20, 'T-20')])
      .mockResolvedValueOnce([]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('reviews-tab-task')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('reviews-tab-task'));
    await waitFor(() => expect(screen.getByTestId('reviews-row-T-20')).toBeInTheDocument());

    fireEvent.click(screen.getByTestId('reviews-approve-T-20'));

    await waitFor(() =>
      expect(submitTaskReview).toHaveBeenCalledWith(20, 'APPROVED', undefined),
    );
  });

  /** TC-TREV-008: Task tab — reject prompts reason and passes it through. */
  it('rejects a task with a reason prompt (TC-TREV-008)', async () => {
    vi.mocked(getPendingReviews)
      .mockResolvedValueOnce([taskReview(21, 'T-21')])
      .mockResolvedValueOnce([]);
    const promptSpy = vi.spyOn(window, 'prompt').mockReturnValue('需要重写');
    renderPage();
    await waitFor(() => expect(screen.getByTestId('reviews-tab-task')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('reviews-tab-task'));
    await waitFor(() => expect(screen.getByTestId('reviews-row-T-21')).toBeInTheDocument());

    fireEvent.click(screen.getByTestId('reviews-reject-T-21'));

    await waitFor(() =>
      expect(submitTaskReview).toHaveBeenCalledWith(21, 'REJECTED', '需要重写'),
    );
    promptSpy.mockRestore();
  });
});
