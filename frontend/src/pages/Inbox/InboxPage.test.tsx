import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { InboxPage } from './InboxPage';
import { getInbox } from '../../api/inbox';

vi.mock('../../api/inbox', async (orig) => ({
  ...(await orig<typeof import('../../api/inbox')>()),
  getInbox: vi.fn(),
}));

function renderPage() {
  return render(
    <MemoryRouter>
      <InboxPage />
    </MemoryRouter>,
  );
}

describe('InboxPage', () => {
  beforeEach(() => {
    vi.mocked(getInbox).mockReset();
  });

  /** TC-INBOXP-01: renders both sections + counts. */
  it('renders unconverted demands and my requirements (TC-INBOXP-01)', async () => {
    vi.mocked(getInbox).mockResolvedValue({
      unconvertedDemands: [{ id: 5, title: '导出报表', priority: 'HIGH', status: 'PENDING' }],
      myRequirements: [
        {
          id: 9,
          code: 'REQ-9',
          title: '登录改造',
          status: 'IN_ANALYSIS',
          priority: 'URGENT',
          expectedDate: '2026-09-01',
          projectId: 1,
          projectName: 'Apollo',
        },
      ],
    });
    renderPage();
    await waitFor(() => expect(screen.getByTestId('inbox-demand-row-5')).toBeInTheDocument());
    expect(screen.getByTestId('inbox-demand-row-5')).toHaveTextContent('导出报表');
    expect(screen.getByTestId('inbox-req-row-9')).toHaveTextContent('REQ-9');
    expect(screen.getByTestId('inbox-req-row-9')).toHaveTextContent('分析中');
    expect(screen.getByTestId('inbox-summary')).toBeInTheDocument();
  });

  /** TC-INBOXP-02: both sections empty → two empty states. */
  it('shows empty states when the inbox is empty (TC-INBOXP-02)', async () => {
    vi.mocked(getInbox).mockResolvedValue({ unconvertedDemands: [], myRequirements: [] });
    renderPage();
    await waitFor(() => expect(screen.getByTestId('inbox-demands-empty')).toBeInTheDocument());
    expect(screen.getByTestId('inbox-reqs-empty')).toBeInTheDocument();
  });
});
