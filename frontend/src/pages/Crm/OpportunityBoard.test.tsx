import { fireEvent, render, screen, waitFor } from '@testing-library/react';
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

  /** TC-OPPB-01: renders the two phase bands + key columns + a WON status chip (size 100). */
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
    // v0.0.45 — 产品标签 shows on the card when set
    expect(screen.getByTestId('opp-product-7')).toHaveTextContent('采购平台');
    expect(screen.getByTestId('opp-summary')).toBeInTheDocument();
  });

  /** TC-OPPB-02: no 流转 controls (新建/推进/通过/否决) — those live on the 流转 pages; only read-only 产出物 view. */
  it('exposes no 流转 controls — board is read-only (TC-OPPB-02)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'OPPORTUNITY')]));
    renderBoard();
    await waitFor(() => expect(screen.getByTestId('opp-card-7')).toBeInTheDocument());
    expect(screen.getByTestId('opp-readonly-hint')).toBeInTheDocument();
    expect(screen.queryByTestId('opp-new-btn')).not.toBeInTheDocument();
    expect(screen.queryByTestId('opp-pass-7')).not.toBeInTheDocument();
    expect(screen.queryByTestId('opp-reject-7')).not.toBeInTheDocument();
    expect(screen.queryByTestId('opp-advance-7')).not.toBeInTheDocument();
    // read-only 产出物 view button IS allowed (viewing/exporting is not a 流转 op)
    expect(screen.getByTestId('opp-artifacts-7')).toBeInTheDocument();
  });

  /** TC-OBA-01: 产出物 button opens a read-only drawer listing artifacts, each with 导出 Word. */
  it('views artifacts and exports Word from the board (TC-OBA-01)', async () => {
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
    await waitFor(() => expect(screen.getByTestId('opp-artifacts-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-artifacts-7'));
    await waitFor(() => expect(listOpportunityArtifacts).toHaveBeenCalledWith(7));
    await waitFor(() => expect(screen.getByTestId('opp-artifact-55')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-export-55'));
    await waitFor(() =>
      expect(exportArtifactDocx).toHaveBeenCalledWith(7, 55, expect.stringContaining('.docx')),
    );
  });
});
