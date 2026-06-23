import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { OpportunityBoard } from './OpportunityBoard';
import { listOpportunities, type Opportunity } from '../../api/opportunity';
import {
  exportArtifactDocx,
  listOpportunityArtifacts,
  type OpportunityArtifact,
} from '../../api/opportunityArtifact';

vi.mock('../../api/opportunity', async (orig) => ({
  ...(await orig<typeof import('../../api/opportunity')>()),
  listOpportunities: vi.fn(),
}));
vi.mock('../../api/opportunityArtifact', () => ({
  listOpportunityArtifacts: vi.fn(() => Promise.resolve([])),
  exportArtifactDocx: vi.fn(() => Promise.resolve()),
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

function daysAgoISO(d: number): string {
  return new Date(Date.now() - d * 86_400_000).toISOString();
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
    vi.mocked(listOpportunityArtifacts).mockClear();
    vi.mocked(exportArtifactDocx).mockClear();
  });

  /** TC-OPPB-01: two stacked phase bands + key columns + a WON status chip (size 100). */
  it('renders the two phase bands and columns (TC-OPPB-01)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(
      page([
        opp(7, 'OPPORTUNITY', { productName: '采购平台' }),
        opp(8, 'INITIATION', { status: 'WON' }),
      ]),
    );
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-card-7')).toBeInTheDocument());
    expect(listOpportunities).toHaveBeenCalledWith({ size: 100 });
    expect(screen.getByTestId('opp-phase-presale')).toBeInTheDocument();
    expect(screen.getByTestId('opp-phase-delivery')).toBeInTheDocument();
    expect(screen.getByTestId('opp-col-LEAD')).toBeInTheDocument();
    expect(screen.getByTestId('opp-col-CONTRACT')).toBeInTheDocument();
    expect(screen.getByTestId('opp-col-INITIATION')).toBeInTheDocument();
    expect(screen.getByTestId('opp-col-ACCEPTANCE')).toBeInTheDocument();
    expect(screen.getByTestId('opp-status-8')).toBeInTheDocument();
    expect(screen.getByTestId('opp-product-7')).toHaveTextContent('采购平台');
    expect(screen.getByTestId('opp-summary')).toBeInTheDocument();
  });

  /** TC-OPPB-02: no 流转 controls; no per-card button — whole card is the affordance. */
  it('exposes no 流转 controls — board is read-only (TC-OPPB-02)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'OPPORTUNITY')]));
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-card-7')).toBeInTheDocument());
    expect(screen.getByTestId('opp-readonly-hint')).toBeInTheDocument();
    expect(screen.queryByTestId('opp-new-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('opp-pass-7')).not.toBeInTheDocument();
    expect(screen.queryByTestId('opp-reject-7')).not.toBeInTheDocument();
    expect(screen.queryByTestId('opp-advance-7')).not.toBeInTheDocument();
    // v0.0.47: per-card 产出物 button removed; the whole card opens the read-only drawer
    expect(screen.queryByTestId('opp-artifacts-7')).not.toBeInTheDocument();
  });

  /** TC-OBA-01: clicking a card opens a read-only 产出物 drawer, each artifact with 导出 Word. */
  it('views artifacts and exports Word by clicking a card (TC-OBA-01)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'OPPORTUNITY')]));
    const art: OpportunityArtifact = {
      id: 55,
      opportunityId: 7,
      type: 'RESEARCH_REPORT',
      typeLabel: '商机调研报告',
      stageFrom: 'LEAD',
      title: '调研',
      content: '正文…',
      author: 'alice',
    };
    vi.mocked(listOpportunityArtifacts).mockResolvedValue([art]);
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-card-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-card-7'));
    await waitFor(() => expect(listOpportunityArtifacts).toHaveBeenCalledWith(7));
    await waitFor(() => expect(screen.getByTestId('opp-artifact-55')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-export-55'));
    await waitFor(() =>
      expect(exportArtifactDocx).toHaveBeenCalledWith(7, 55, expect.stringContaining('.docx')),
    );
  });

  /** TC-OPPB-03: funnel strip shows per-stage counts. */
  it('shows a funnel distribution strip with stage counts (TC-OPPB-03)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(
      page([opp(7, 'OPPORTUNITY'), opp(8, 'OPPORTUNITY'), opp(9, 'BIDDING')]),
    );
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-funnel')).toBeInTheDocument());
    expect(screen.getByTestId('opp-funnel-OPPORTUNITY')).toHaveTextContent('2');
    expect(screen.getByTestId('opp-funnel-BIDDING')).toHaveTextContent('1');
    expect(screen.getByTestId('opp-funnel-LEAD')).toHaveTextContent('0');
  });

  /** TC-OPPB-04: card amount is formatted (元 → 万/亿). */
  it('formats the card amount (TC-OPPB-04)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'OPPORTUNITY', { amount: 2000000 })]));
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-card-7')).toBeInTheDocument());
    expect(screen.getByTestId('opp-card-7')).toHaveTextContent('¥200万');
  });

  /** TC-OPPB-05: filter by owner narrows the cards. */
  it('filters by owner (TC-OPPB-05)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(
      page([opp(7, 'OPPORTUNITY', { pmName: '王伟' }), opp(8, 'OPPORTUNITY', { pmName: '李娜' })]),
    );
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-card-8')).toBeInTheDocument());
    fireEvent.change(screen.getByTestId('opp-filter-owner'), { target: { value: '王伟' } });
    await waitFor(() => expect(screen.queryByTestId('opp-card-8')).not.toBeInTheDocument());
    expect(screen.getByTestId('opp-card-7')).toBeInTheDocument();
  });

  /** TC-OPPB-06: LOST hidden by default; the 丢单 toggle reveals them with a LOST chip. */
  it('toggles LOST visibility (TC-OPPB-06)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(
      page([opp(7, 'OPPORTUNITY'), opp(8, 'BIDDING', { status: 'LOST' })]),
    );
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-card-7')).toBeInTheDocument());
    expect(screen.queryByTestId('opp-card-8')).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId('opp-tile-lost'));
    await waitFor(() => expect(screen.getByTestId('opp-card-8')).toBeInTheDocument());
    expect(screen.getByTestId('opp-status-8')).toHaveTextContent('丢单');
  });

  /** TC-OPPB-07: toggle between board and list views. */
  it('switches between board and list views (TC-OPPB-07)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'OPPORTUNITY')]));
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-phase-presale')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-view-list'));
    await waitFor(() => expect(screen.getByTestId('opp-list')).toBeInTheDocument());
    expect(screen.queryByTestId('opp-phase-presale')).not.toBeInTheDocument();
    expect(within(screen.getByTestId('opp-list')).getByTestId('opp-list-row-7')).toBeInTheDocument();
    fireEvent.click(screen.getByTestId('opp-view-board'));
    await waitFor(() => expect(screen.getByTestId('opp-phase-presale')).toBeInTheDocument());
  });

  /** TC-OPPB-08: dwell warning dot grades by days in stage (gray/green/yellow/red). */
  it('renders a dwell warning dot graded by days (TC-OPPB-08)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(
      page([
        opp(1, 'OPPORTUNITY', { stageEnteredAt: daysAgoISO(3) }), // green
        opp(2, 'OPPORTUNITY', { stageEnteredAt: daysAgoISO(10) }), // yellow
        opp(3, 'OPPORTUNITY', { stageEnteredAt: daysAgoISO(20) }), // red
        opp(4, 'OPPORTUNITY', {}), // no stageEnteredAt → gray
      ]),
    );
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-dwell-1')).toBeInTheDocument());
    expect(screen.getByTestId('opp-dwell-1')).toHaveAttribute('data-tier', 'green');
    expect(screen.getByTestId('opp-dwell-2')).toHaveAttribute('data-tier', 'yellow');
    expect(screen.getByTestId('opp-dwell-3')).toHaveAttribute('data-tier', 'red');
    expect(screen.getByTestId('opp-dwell-4')).toHaveAttribute('data-tier', 'gray');
  });

  /** TC-OPPB-09: list view sorts by amount/dwell + shows dwell dot + OPEN→'进行中'. */
  it('sorts the list view and renders list dwell/status cells (TC-OPPB-09)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(
      page([
        opp(1, 'OPPORTUNITY', { amount: 100000, stageEnteredAt: daysAgoISO(20) }),
        opp(2, 'BIDDING', { amount: 5000000, stageEnteredAt: daysAgoISO(2) }),
        opp(3, 'POC', { amount: 800000, stageEnteredAt: daysAgoISO(10) }),
      ]),
    );
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-view-list')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-view-list'));
    await waitFor(() => expect(screen.getByTestId('opp-list')).toBeInTheDocument());
    // OPEN status cell renders 进行中; list dwell cell carries the tier
    expect(within(screen.getByTestId('opp-list-row-1')).getByText('进行中')).toBeInTheDocument();
    expect(within(screen.getByTestId('opp-list-row-1')).getByTestId('opp-dwell-1')).toHaveAttribute('data-tier', 'red');
    // sort by amount desc → row 2 (¥500万) first
    fireEvent.change(screen.getByTestId('opp-list-sort'), { target: { value: 'amount' } });
    let ids = screen.getAllByTestId(/opp-list-row-/).map((el) => el.getAttribute('data-testid'));
    expect(ids).toEqual(['opp-list-row-2', 'opp-list-row-3', 'opp-list-row-1']);
    // sort by dwell desc → row 1 (20d) first, row 2 (2d) last
    fireEvent.change(screen.getByTestId('opp-list-sort'), { target: { value: 'dwell' } });
    ids = screen.getAllByTestId(/opp-list-row-/).map((el) => el.getAttribute('data-testid'));
    expect(ids).toEqual(['opp-list-row-1', 'opp-list-row-3', 'opp-list-row-2']);
  });

  /** TC-OPPB-10: filter by product narrows the cards. */
  it('filters by product (TC-OPPB-10)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(
      page([
        opp(7, 'OPPORTUNITY', { productName: '采购平台' }),
        opp(8, 'OPPORTUNITY', { productName: '风控系统' }),
      ]),
    );
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-card-8')).toBeInTheDocument());
    fireEvent.change(screen.getByTestId('opp-filter-product'), { target: { value: '采购平台' } });
    await waitFor(() => expect(screen.queryByTestId('opp-card-8')).not.toBeInTheDocument());
    expect(screen.getByTestId('opp-card-7')).toBeInTheDocument();
  });

  /** TC-OPPB-11: customer search is case-insensitive substring. */
  it('searches customer name case-insensitively (TC-OPPB-11)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(
      page([
        opp(7, 'OPPORTUNITY', { customerName: 'ACME 集团' }),
        opp(8, 'OPPORTUNITY', { customerName: '蓝海物流' }),
      ]),
    );
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-card-8')).toBeInTheDocument());
    fireEvent.change(screen.getByTestId('opp-filter-q'), { target: { value: 'acme' } });
    await waitFor(() => expect(screen.queryByTestId('opp-card-8')).not.toBeInTheDocument());
    expect(screen.getByTestId('opp-card-7')).toBeInTheDocument();
  });
});
