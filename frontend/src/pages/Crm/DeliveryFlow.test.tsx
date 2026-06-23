import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { DeliveryFlow } from './DeliveryFlow';
import {
  advanceOpportunity,
  initiateOpportunity,
  listOpportunities,
  type Opportunity,
} from '../../api/opportunity';
import { listProjects } from '../../api/project';

vi.mock('../../api/opportunity', async (orig) => ({
  ...(await orig<typeof import('../../api/opportunity')>()),
  listOpportunities: vi.fn(),
  advanceOpportunity: vi.fn(() => Promise.resolve({} as Opportunity)),
  initiateOpportunity: vi.fn(() => Promise.resolve({} as Opportunity)),
}));
vi.mock('../../api/project', () => ({
  listProjects: vi.fn().mockResolvedValue({
    content: [{ id: 3, code: 'PRJ-3', name: '交付项目三', status: 'ACTIVE' }],
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
    status: 'WON',
    pmName: '王伟',
    ...over,
  };
}

function page(rows: Opportunity[]) {
  return { content: rows, total: rows.length, page: 0, size: 100 };
}

function renderPage() {
  return render(
    <MemoryRouter>
      <DeliveryFlow />
    </MemoryRouter>,
  );
}

describe('DeliveryFlow (实施流转 operations)', () => {
  beforeEach(() => {
    vi.mocked(advanceOpportunity).mockClear();
    vi.mocked(initiateOpportunity).mockClear();
    vi.mocked(listProjects).mockClear();
    vi.mocked(listOpportunities).mockReset();
  });

  /** TC-DEL-01: lists only WON 实施 opps; 立项→移交+通过/否决, non-gate→推进, 验收→已验收, OPEN/售前 excluded. */
  it('lists WON 实施 opps with stage-appropriate actions (TC-DEL-01)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(
      page([
        opp(7, 'INITIATION'), // gate → 立项移交 + 通过/否决
        opp(8, 'SURVEY'), // non-gate → 推进
        opp(9, 'ACCEPTANCE'), // terminal → 已验收
        opp(10, 'OPPORTUNITY', { status: 'OPEN' }), // 售前/OPEN → excluded
        opp(11, 'CONTRACT', { status: 'WON' }), // WON but 售前 stage → excluded by STAGE filter (not just status)
      ]),
    );
    renderPage();
    await waitFor(() => expect(screen.getByTestId('delivery-row-7')).toBeInTheDocument());
    expect(listOpportunities).toHaveBeenCalledWith({ size: 100 });
    expect(screen.getByTestId('delivery-handoff-7')).toBeInTheDocument();
    expect(screen.getByTestId('delivery-pass-7')).toBeInTheDocument();
    expect(screen.getByTestId('delivery-reject-7')).toBeInTheDocument();
    expect(screen.getByTestId('delivery-advance-8')).toBeInTheDocument();
    expect(screen.getByTestId('delivery-done-9')).toBeInTheDocument();
    // ACCEPTANCE is terminal — no advance control
    expect(screen.queryByTestId('delivery-advance-9')).not.toBeInTheDocument();
    // 售前/OPEN not on the 实施 page
    expect(screen.queryByTestId('delivery-row-10')).not.toBeInTheDocument();
    // STAGE filter is independent of status: a WON 售前-stage opp is also excluded
    expect(screen.queryByTestId('delivery-row-11')).not.toBeInTheDocument();
  });

  /** TC-DEL-02: 通过 at 立项 calls advance(id,'PASS') and refetches. */
  it('passes the 立项 gate and refetches (TC-DEL-02)', async () => {
    vi.mocked(listOpportunities)
      .mockResolvedValueOnce(page([opp(7, 'INITIATION')]))
      .mockResolvedValueOnce(page([opp(7, 'SURVEY')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('delivery-pass-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('delivery-pass-7'));
    await waitFor(() => expect(advanceOpportunity).toHaveBeenCalledWith(7, 'PASS'));
    await waitFor(() => expect(screen.getByTestId('delivery-advance-7')).toBeInTheDocument());
    expect(listOpportunities).toHaveBeenCalledTimes(2);
  });

  /** TC-DEL-03: 立项移交 opens a Project picker; 移交 links the Project via initiate(id, projectId, 'PASS'). */
  it('hands off to a delivery Project via initiate (TC-DEL-03)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'INITIATION')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('delivery-handoff-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('delivery-handoff-7'));
    await waitFor(() => expect(screen.getByTestId('delivery-project-select')).toBeInTheDocument());
    expect(listProjects).toHaveBeenCalled();
    fireEvent.change(screen.getByTestId('delivery-project-select'), { target: { value: '3' } });
    fireEvent.click(screen.getByTestId('delivery-handoff-save'));
    await waitFor(() => expect(initiateOpportunity).toHaveBeenCalledWith(7, 3, 'PASS'));
  });

  /** TC-DEL-04: 立项移交 drawer with no available projects shows guidance instead of an empty select. */
  it('shows an empty-state when no projects are available (TC-DEL-04)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'INITIATION')]));
    vi.mocked(listProjects).mockResolvedValueOnce({ content: [], total: 0, page: 0, size: 100 });
    renderPage();
    await waitFor(() => expect(screen.getByTestId('delivery-handoff-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('delivery-handoff-7'));
    await waitFor(() => expect(screen.getByTestId('delivery-no-projects')).toBeInTheDocument());
    // no select rendered, and 移交 stays disabled (projectId can't be chosen)
    expect(screen.queryByTestId('delivery-project-select')).not.toBeInTheDocument();
    expect(screen.getByTestId('delivery-handoff-save')).toBeDisabled();
    expect(initiateOpportunity).not.toHaveBeenCalled();
  });
});
