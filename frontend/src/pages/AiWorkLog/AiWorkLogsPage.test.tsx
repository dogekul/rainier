import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AiWorkLogsPage } from './AiWorkLogsPage';
import { decideAiWorkLog, listAiWorkLogs, type AiWorkLog } from '../../api/aiWorkLog';

vi.mock('../../api/aiWorkLog', async (orig) => ({
  ...(await orig<typeof import('../../api/aiWorkLog')>()),
  listAiWorkLogs: vi.fn(),
  decideAiWorkLog: vi.fn(() => Promise.resolve({} as AiWorkLog)),
}));

function log(id: number, status: AiWorkLog['status'] = 'PROPOSED'): AiWorkLog {
  return {
    id,
    agentType: 'STATUS_SYNC',
    action: 'UPDATE_TASK_STATUS',
    summary: '建议关闭任务',
    evidence: 'commit abc123',
    status,
  };
}

function page(rows: AiWorkLog[]) {
  return { content: rows, total: rows.length, page: 0, size: 100 };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <AiWorkLogsPage />
    </MemoryRouter>,
  );
}

describe('AiWorkLogsPage', () => {
  beforeEach(() => {
    vi.mocked(decideAiWorkLog).mockClear();
    vi.mocked(listAiWorkLogs).mockReset();
  });

  /** TC-AIWP-01: renders a proposal with accept/reject buttons. */
  it('renders proposals with accept/reject (TC-AIWP-01)', async () => {
    vi.mocked(listAiWorkLogs).mockResolvedValue(page([log(7)]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('ai-row-7')).toBeInTheDocument());
    expect(screen.getByTestId('ai-row-7')).toHaveTextContent('commit abc123');
    expect(screen.getByTestId('ai-accept-7')).toBeInTheDocument();
    expect(screen.getByTestId('ai-reject-7')).toBeInTheDocument();
    expect(screen.getByTestId('ai-summary')).toBeInTheDocument();
  });

  /** TC-AIWP-02: accepting calls decide(id, ACCEPTED) and refetches. */
  it('accepts a proposal and refetches (TC-AIWP-02)', async () => {
    vi.mocked(listAiWorkLogs)
      .mockResolvedValueOnce(page([log(7)]))
      .mockResolvedValueOnce(page([]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('ai-row-7')).toBeInTheDocument());

    fireEvent.click(screen.getByTestId('ai-accept-7'));

    await waitFor(() => expect(decideAiWorkLog).toHaveBeenCalledWith(7, 'ACCEPTED'));
    await waitFor(() => expect(screen.getByTestId('ai-empty')).toBeInTheDocument());
    expect(listAiWorkLogs).toHaveBeenCalledTimes(2);
  });

  /** TC-AIWP-03: empty list → empty state. */
  it('shows an empty state when there are no logs (TC-AIWP-03)', async () => {
    vi.mocked(listAiWorkLogs).mockResolvedValue(page([]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('ai-empty')).toBeInTheDocument());
  });
});
