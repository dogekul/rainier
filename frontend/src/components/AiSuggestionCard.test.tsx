import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AiSuggestionCard } from './AiSuggestionCard';
import {
  acceptWorkLog,
  listMyProposals,
  rejectWorkLog,
  reverseWorkLog,
  type AiWorkLog,
} from '../api/aiWorkLog';

vi.mock('../api/aiWorkLog', async (orig) => ({
  ...(await orig<typeof import('../api/aiWorkLog')>()),
  listMyProposals: vi.fn(),
  acceptWorkLog: vi.fn(),
  rejectWorkLog: vi.fn(),
  reverseWorkLog: vi.fn(),
}));

function row(id: number, evidence: string = '{}'): AiWorkLog {
  return {
    id,
    agentType: 'STATUS_SYNC',
    action: 'UPDATE_TASK_STATUS',
    summary: '建议关闭任务 #' + id,
    evidence,
    status: 'PROPOSED',
  };
}

describe('AiSuggestionCard (F4 TC-AI-CARD)', () => {
  beforeEach(() => {
    vi.mocked(listMyProposals).mockReset();
    vi.mocked(acceptWorkLog).mockReset();
    vi.mocked(rejectWorkLog).mockReset();
    vi.mocked(reverseWorkLog).mockReset();
  });

  /** TC-AI-CARD-01: renders ≤3 PROPOSED rows, parses evidence eventId/source for the back-pointer. */
  it('lists PROPOSED proposals and shows the 事件 #N back-pointer', async () => {
    vi.mocked(listMyProposals).mockResolvedValue([
      row(1, JSON.stringify({ eventId: 42, source: 'GITLAB' })),
      row(2),
      row(3, '{not-json}'),
    ]);

    render(<AiSuggestionCard />);

    await waitFor(() => expect(screen.getByTestId('ai-suggest-row-1')).toBeInTheDocument());
    expect(screen.getByTestId('ai-suggest-row-2')).toBeInTheDocument();
    expect(screen.getByTestId('ai-suggest-row-3')).toBeInTheDocument();
    expect(screen.getByTestId('ai-suggest-evidence-1')).toHaveTextContent('事件 #42 (GITLAB)');
    expect(screen.queryByTestId('ai-suggest-evidence-2')).not.toBeInTheDocument();
    expect(screen.queryByTestId('ai-suggest-evidence-3')).not.toBeInTheDocument();
  });

  /** TC-AI-CARD-02: 采纳 → row flips to 已采纳 + 撤销 button; 撤销 calls /reverse and refreshes. */
  it('accepts then allows 撤销 within the 5s window (TC-AI-CARD-02)', async () => {
    vi.mocked(listMyProposals).mockResolvedValueOnce([row(11)]);
    vi.mocked(acceptWorkLog).mockResolvedValue({ ...row(11), status: 'ACCEPTED' });
    vi.mocked(reverseWorkLog).mockResolvedValue({ ...row(11), status: 'PROPOSED' });
    vi.mocked(listMyProposals).mockResolvedValueOnce([]); // post-reverse refresh

    render(<AiSuggestionCard />);
    await waitFor(() => expect(screen.getByTestId('ai-suggest-row-11')).toBeInTheDocument());

    fireEvent.click(screen.getByTestId('ai-suggest-accept-11'));
    await waitFor(() => expect(acceptWorkLog).toHaveBeenCalledWith(11));
    await waitFor(() => expect(screen.getByTestId('ai-suggest-undo-11')).toBeInTheDocument());

    fireEvent.click(screen.getByTestId('ai-suggest-undo-11'));
    await waitFor(() => expect(reverseWorkLog).toHaveBeenCalledWith(11));
    await waitFor(() =>
      expect(screen.queryByTestId('ai-suggest-row-11')).not.toBeInTheDocument(),
    );
  });

  /** TC-AI-CARD-03: 驳回 needs an inline reason; empty reason shows an error, valid one calls API. */
  it('rejects with an inline reason and refreshes the list (TC-AI-CARD-03)', async () => {
    vi.mocked(listMyProposals).mockResolvedValueOnce([row(21)]);
    vi.mocked(rejectWorkLog).mockResolvedValue({ ...row(21), status: 'REJECTED' });
    vi.mocked(listMyProposals).mockResolvedValueOnce([]); // post-reject refresh

    render(<AiSuggestionCard />);
    await waitFor(() => expect(screen.getByTestId('ai-suggest-row-21')).toBeInTheDocument());

    fireEvent.click(screen.getByTestId('ai-suggest-reject-21'));
    await waitFor(() =>
      expect(screen.getByTestId('ai-suggest-reject-form-21')).toBeInTheDocument(),
    );

    // empty reason → inline error
    fireEvent.click(screen.getByTestId('ai-suggest-reject-submit-21'));
    await waitFor(() =>
      expect(screen.getByTestId('ai-suggest-reject-err-21')).toBeInTheDocument(),
    );
    expect(rejectWorkLog).not.toHaveBeenCalled();

    // valid reason → API + refresh
    fireEvent.change(screen.getByTestId('ai-suggest-reject-reason-21'), {
      target: { value: '误判' },
    });
    fireEvent.click(screen.getByTestId('ai-suggest-reject-submit-21'));
    await waitFor(() => expect(rejectWorkLog).toHaveBeenCalledWith(21, '误判'));
    await waitFor(() =>
      expect(screen.queryByTestId('ai-suggest-row-21')).not.toBeInTheDocument(),
    );
  });

  /** TC-AI-CARD-04: empty list shows the empty hint. */
  it('shows the empty hint when no PROPOSED rows are returned', async () => {
    vi.mocked(listMyProposals).mockResolvedValue([]);
    render(<AiSuggestionCard />);
    await waitFor(() => expect(screen.getByTestId('ai-suggest-empty')).toBeInTheDocument());
  });
});
