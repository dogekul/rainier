import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { OpportunityBoard } from './OpportunityBoard';
import { listOpportunities, type Opportunity } from '../../api/opportunity';

vi.mock('../../api/opportunity', async (orig) => ({
  ...(await orig<typeof import('../../api/opportunity')>()),
  listOpportunities: vi.fn(),
}));

function opp(id: number, stage: Opportunity['stage'], over: Partial<Opportunity> = {}): Opportunity {
  return {
    id,
    customerName: 'X 集团',
    title: '采购系统',
    amount: 100000,
    stage,
    status: 'OPEN',
    pmName: '王伟',
    ...over,
  };
}

function page(rows: Opportunity[]) {
  return { content: rows, total: rows.length, page: 0, size: 100 };
}

function renderBoard() {
  return render(
    <MemoryRouter>
      <OpportunityBoard />
    </MemoryRouter>,
  );
}

describe('OpportunityBoard (read-only)', () => {
  beforeEach(() => {
    vi.mocked(listOpportunities).mockReset();
  });

  /** TC-OPPB-01: renders the two phase bands + key columns + a WON status chip (size 100). */
  it('renders the two phase bands and columns (TC-OPPB-01)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(
      page([opp(7, 'OPPORTUNITY'), opp(8, 'INITIATION', { status: 'WON' })]),
    );
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-card-7')).toBeInTheDocument());
    // PageParams caps size at 100 — requesting more 400s (regression guard for the size=200 bug).
    expect(listOpportunities).toHaveBeenCalledWith({ size: 100 });
    // two phase bands (售前/实施)
    expect(screen.getByTestId('opp-phase-presale')).toBeInTheDocument();
    expect(screen.getByTestId('opp-phase-delivery')).toBeInTheDocument();
    // 售前 first + last column and 实施 first + last column
    expect(screen.getByTestId('opp-col-LEAD')).toBeInTheDocument();
    expect(screen.getByTestId('opp-col-CONTRACT')).toBeInTheDocument();
    expect(screen.getByTestId('opp-col-INITIATION')).toBeInTheDocument();
    expect(screen.getByTestId('opp-col-ACCEPTANCE')).toBeInTheDocument();
    // WON card carries a 赢单 status chip
    expect(screen.getByTestId('opp-status-8')).toBeInTheDocument();
    expect(screen.getByTestId('opp-summary')).toBeInTheDocument();
  });

  /** TC-OPPB-02: the board is READ-ONLY — no 新建/推进/通过/否决 controls (those moved to 流转 pages). */
  it('exposes no action controls — board is read-only (TC-OPPB-02)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'OPPORTUNITY')]));
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-card-7')).toBeInTheDocument());
    expect(screen.getByTestId('opp-readonly-hint')).toBeInTheDocument();
    expect(screen.queryByTestId('opp-new-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('opp-pass-7')).not.toBeInTheDocument();
    expect(screen.queryByTestId('opp-reject-7')).not.toBeInTheDocument();
    expect(screen.queryByTestId('opp-advance-7')).not.toBeInTheDocument();
    // Belt-and-suspenders: the board is rendered in isolation (no app shell), so a count of 0 buttons
    // proves read-only even if a future action is added with a testid this test doesn't yet name.
    expect(screen.queryAllByRole('button')).toHaveLength(0);
  });
});
