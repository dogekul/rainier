import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PresaleFlow } from './PresaleFlow';
import {
  advanceOpportunity,
  createOpportunity,
  listOpportunities,
  type Opportunity,
} from '../../api/opportunity';

vi.mock('../../api/opportunity', async (orig) => ({
  ...(await orig<typeof import('../../api/opportunity')>()),
  listOpportunities: vi.fn(),
  advanceOpportunity: vi.fn(() => Promise.resolve({} as Opportunity)),
  createOpportunity: vi.fn(() => Promise.resolve({} as Opportunity)),
}));
vi.mock('../../api/user', () => ({
  listUsers: vi.fn().mockResolvedValue({
    content: [{ id: 5, loginName: 'li', name: '李商务', isInternal: true, enabled: true }],
    total: 1,
    page: 0,
    size: 100,
  }),
}));

function opp(id: number, stage: Opportunity['stage'], over: Partial<Opportunity> = {}): Opportunity {
  return {
    id,
    customerName: 'X 集团',
    title: '采购系统',
    amount: 100000,
    stage,
    status: 'OPEN',
    commercialOwnerName: '李商务',
    ...over,
  };
}

function page(rows: Opportunity[]) {
  return { content: rows, total: rows.length, page: 0, size: 100 };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <PresaleFlow />
    </MemoryRouter>,
  );
}

describe('PresaleFlow (售前流转 operations)', () => {
  beforeEach(() => {
    vi.mocked(advanceOpportunity).mockClear();
    vi.mocked(createOpportunity).mockClear();
    vi.mocked(listOpportunities).mockReset();
  });

  /** TC-PRE-01: lists only OPEN 售前 opps; gate rows show 通过/否决, non-gate show 推进; WON/实施 filtered out. */
  it('lists OPEN 售前 opps with stage-appropriate actions (TC-PRE-01)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(
      page([
        opp(7, 'OPPORTUNITY'), // gate
        opp(8, 'LEAD'), // non-gate
        opp(9, 'INITIATION', { status: 'WON' }), // 实施 (WON) → excluded
        opp(10, 'BIDDING', { status: 'LOST' }), // lost → excluded
        opp(11, 'SURVEY', { status: 'OPEN' }), // OPEN but 实施 stage → excluded by STAGE filter (not just status)
      ]),
    );
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-row-7')).toBeInTheDocument());
    expect(listOpportunities).toHaveBeenCalledWith({ size: 100 });
    // gate row → 通过/否决
    expect(screen.getByTestId('presale-pass-7')).toBeInTheDocument();
    expect(screen.getByTestId('presale-reject-7')).toBeInTheDocument();
    // non-gate row → 推进
    expect(screen.getByTestId('presale-advance-8')).toBeInTheDocument();
    // 实施 (WON) and LOST are NOT on the 售前 page
    expect(screen.queryByTestId('presale-row-9')).not.toBeInTheDocument();
    expect(screen.queryByTestId('presale-row-10')).not.toBeInTheDocument();
    // STAGE filter is independent of status: an OPEN 实施-stage opp is also excluded
    expect(screen.queryByTestId('presale-row-11')).not.toBeInTheDocument();
  });

  /** TC-PRE-02: 通过 a gate calls advance(id,'PASS') and refetches. */
  it('passes a gate and refetches (TC-PRE-02)', async () => {
    vi.mocked(listOpportunities)
      .mockResolvedValueOnce(page([opp(7, 'OPPORTUNITY')]))
      .mockResolvedValueOnce(page([opp(7, 'POC')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-pass-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-pass-7'));
    await waitFor(() => expect(advanceOpportunity).toHaveBeenCalledWith(7, 'PASS'));
    await waitFor(() => expect(screen.getByTestId('presale-advance-7')).toBeInTheDocument());
    expect(listOpportunities).toHaveBeenCalledTimes(2);
  });

  /** TC-PRE-03: 新建商机 drawer renders the 4 owner selects + creates with all filled fields (incl. owner). */
  it('creates an opportunity with an owner via the drawer (TC-PRE-03)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-empty')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-new-btn'));
    await waitFor(() => expect(screen.getByTestId('presale-new-customer')).toBeInTheDocument());
    // all 4 owner selects render (商务/解决方案/PM/运营)
    expect(screen.getByTestId('presale-owner-commercial')).toBeInTheDocument();
    expect(screen.getByTestId('presale-owner-solution')).toBeInTheDocument();
    expect(screen.getByTestId('presale-owner-pm')).toBeInTheDocument();
    expect(screen.getByTestId('presale-owner-ops')).toBeInTheDocument();
    fireEvent.change(screen.getByTestId('presale-new-customer'), { target: { value: '远方集团' } });
    fireEvent.change(screen.getByTestId('presale-new-title'), { target: { value: '对外交付' } });
    fireEvent.change(screen.getByTestId('presale-new-amount'), { target: { value: '500000' } });
    // pick a commercial owner once the user list has loaded
    await waitFor(() =>
      expect(screen.getByTestId('presale-owner-commercial')).toHaveTextContent('李商务'),
    );
    fireEvent.change(screen.getByTestId('presale-owner-commercial'), { target: { value: '5' } });
    fireEvent.click(screen.getByTestId('presale-save-btn'));
    await waitFor(() =>
      expect(createOpportunity).toHaveBeenCalledWith(
        expect.objectContaining({
          customerName: '远方集团',
          title: '对外交付',
          amount: 500000,
          commercialOwnerUserId: 5,
        }),
      ),
    );
  });

  /** TC-PRE-04: empty required fields → form error, no create. */
  it('shows a form error when required fields are empty (TC-PRE-04)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-empty')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-new-btn'));
    await waitFor(() => expect(screen.getByTestId('presale-save-btn')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-save-btn'));
    await waitFor(() => expect(screen.getByTestId('presale-form-error')).toBeInTheDocument());
    expect(createOpportunity).not.toHaveBeenCalled();
  });

  /** TC-PRE-05: 否决 (→丢单) is destructive — it confirms first, then advances with REJECT only on 确认. */
  it('confirms before 否决 (REJECT → LOST) (TC-PRE-05)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'OPPORTUNITY')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-reject-7')).toBeInTheDocument());
    // clicking 否决 does NOT immediately advance — it opens a confirm dialog
    fireEvent.click(screen.getByTestId('presale-reject-7'));
    await waitFor(() => expect(screen.getByTestId('rainier-confirm')).toBeInTheDocument());
    expect(advanceOpportunity).not.toHaveBeenCalled();
    // confirming triggers the REJECT
    fireEvent.click(screen.getByText('确认否决'));
    await waitFor(() => expect(advanceOpportunity).toHaveBeenCalledWith(7, 'REJECT'));
  });
});
