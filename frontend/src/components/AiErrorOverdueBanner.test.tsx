import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AiErrorOverdueBanner } from './AiErrorOverdueBanner';
import { fetchAiErrorOverdueCount } from '../api/aiErrors';

vi.mock('../api/aiErrors', async (orig) => ({
  ...(await orig<typeof import('../api/aiErrors')>()),
  fetchAiErrorOverdueCount: vi.fn(),
}));

function renderBanner() {
  return render(
    <MemoryRouter>
      <AiErrorOverdueBanner />
    </MemoryRouter>,
  );
}

describe('AiErrorOverdueBanner (F5 TC-AI-OVERDUE-BANNER)', () => {
  beforeEach(() => {
    vi.mocked(fetchAiErrorOverdueCount).mockReset();
  });

  /** TC-AI-OVERDUE-BANNER-01: count > 0 → red banner with the number and a link to /ai/errors. */
  it('renders the banner when count > 0', async () => {
    vi.mocked(fetchAiErrorOverdueCount).mockResolvedValue({ count: 2, thresholdHours: 24 });
    renderBanner();

    await waitFor(() =>
      expect(screen.getByTestId('ai-error-overdue-banner')).toBeInTheDocument(),
    );
    expect(screen.getByTestId('ai-error-overdue-banner-count')).toHaveTextContent('2');
    const link = screen.getByTestId('ai-error-overdue-banner-link');
    expect(link).toHaveAttribute('href', '/ai/errors');
    expect(link).toHaveTextContent('查看公示板');
  });

  /** TC-AI-OVERDUE-BANNER-02: count === 0 → no banner in the DOM. */
  it('does not render anything when count is 0', async () => {
    vi.mocked(fetchAiErrorOverdueCount).mockResolvedValue({ count: 0, thresholdHours: 24 });
    renderBanner();

    // Give the effect a tick — the resolved promise must settle before we assert absence.
    await waitFor(() => expect(fetchAiErrorOverdueCount).toHaveBeenCalled());
    expect(screen.queryByTestId('ai-error-overdue-banner')).not.toBeInTheDocument();
  });

  /** TC-AI-OVERDUE-BANNER-03: API error is swallowed, no crash, no banner. */
  it('swallows API errors and renders nothing', async () => {
    vi.mocked(fetchAiErrorOverdueCount).mockRejectedValue(new Error('boom'));
    renderBanner();

    await waitFor(() => expect(fetchAiErrorOverdueCount).toHaveBeenCalled());
    expect(screen.queryByTestId('ai-error-overdue-banner')).not.toBeInTheDocument();
  });
});
