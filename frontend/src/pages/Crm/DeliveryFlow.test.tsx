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
import { listUsers } from '../../api/user';
import {
  createOpportunityArtifact,
  listOpportunityArtifacts,
  type OpportunityArtifact,
} from '../../api/opportunityArtifact';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (orig) => ({
  ...(await orig<typeof import('react-router-dom')>()),
  useNavigate: () => mockNavigate,
}));
vi.mock('../../api/opportunity', async (orig) => ({
  ...(await orig<typeof import('../../api/opportunity')>()),
  listOpportunities: vi.fn(),
  advanceOpportunity: vi.fn(() => Promise.resolve({} as Opportunity)),
  initiateOpportunity: vi.fn(() => Promise.resolve({} as Opportunity)),
}));
vi.mock('../../api/opportunityArtifact', async (orig) => ({
  ...(await orig<typeof import('../../api/opportunityArtifact')>()),
  listOpportunityArtifacts: vi.fn(() => Promise.resolve([] as OpportunityArtifact[])),
  createOpportunityArtifact: vi.fn(() => Promise.resolve({} as OpportunityArtifact)),
}));
vi.mock('../../api/project', async (orig) => ({
  ...(await orig<typeof import('../../api/project')>()),
  listProjects: vi.fn().mockResolvedValue({
    content: [
      { id: 3, code: 'PRJ-3', name: '交付项目三', status: 'ACTIVE', projectType: 'EXTERNAL_DELIVERY' },
    ],
    total: 1,
    page: 0,
    size: 100,
  }),
}));
vi.mock('../../api/user', () => ({
  listUsers: vi.fn().mockResolvedValue({
    content: [
      { id: 5, loginName: 'pm1', name: '项目经理一' },
      { id: 6, loginName: 'pm2', name: '项目经理二' },
    ],
    total: 2,
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
    vi.mocked(listUsers).mockClear();
    vi.mocked(listOpportunities).mockReset();
    vi.mocked(listOpportunityArtifacts).mockClear().mockResolvedValue([] as OpportunityArtifact[]);
    vi.mocked(createOpportunityArtifact).mockClear();
    mockNavigate.mockClear();
  });

  /** TC-DEL-01: lists only WON 实施 opps; 立项→移交+驳回（无独立「通过」）, non-gate→推进, 验收→已验收, OPEN/售前 excluded. */
  it('lists WON 实施 opps with stage-appropriate actions (TC-DEL-01)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(
      page([
        opp(7, 'INITIATION'), // gate → 立项移交 + 驳回（v0.0.52 去掉独立「通过」）
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
    expect(screen.getByTestId('delivery-reject-7')).toBeInTheDocument();
    // v0.0.52: 立项行不再有独立「通过」(立项移交即推进)
    expect(screen.queryByTestId('delivery-pass-7')).not.toBeInTheDocument();
    expect(screen.getByTestId('delivery-advance-8')).toBeInTheDocument();
    expect(screen.getByTestId('delivery-done-9')).toBeInTheDocument();
    // ACCEPTANCE is terminal — no advance control
    expect(screen.queryByTestId('delivery-advance-9')).not.toBeInTheDocument();
    // 售前/OPEN not on the 实施 page
    expect(screen.queryByTestId('delivery-row-10')).not.toBeInTheDocument();
    // STAGE filter is independent of status: a WON 售前-stage opp is also excluded
    expect(screen.queryByTestId('delivery-row-11')).not.toBeInTheDocument();
  });

  /** TC-DEL-02: 现场调研「推进」缺产出物 → 打开补充表单（报告+附件），不立即 advance。 */
  it('opens the supplement form for 现场调研 instead of advancing (TC-DEL-02)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'SURVEY')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('delivery-advance-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('delivery-advance-7'));
    await waitFor(() =>
      expect(screen.getByTestId('delivery-supp-SURVEY_REPORT')).toBeInTheDocument(),
    );
    expect(screen.getByTestId('delivery-supp-SURVEY_ATTACHMENT')).toBeInTheDocument();
    expect(advanceOpportunity).not.toHaveBeenCalled();
  });

  /** TC-DEL-08: 补充表单填报告正文+附件链接，提交 → 逐个建档 + advance(id) + 刷新。 */
  it('submits 现场调研 报告+附件 then advances (TC-DEL-08)', async () => {
    vi.mocked(listOpportunities)
      .mockResolvedValueOnce(page([opp(7, 'SURVEY')]))
      .mockResolvedValueOnce(page([opp(7, 'REQUIREMENT')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('delivery-advance-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('delivery-advance-7'));
    await waitFor(() =>
      expect(screen.getByTestId('delivery-supp-content-SURVEY_REPORT')).toBeInTheDocument(),
    );
    fireEvent.change(screen.getByTestId('delivery-supp-content-SURVEY_REPORT'), {
      target: { value: '现场走访纪要' },
    });
    fireEvent.change(screen.getByTestId('delivery-supp-link-SURVEY_ATTACHMENT-0'), {
      target: { value: 'https://x/site.jpg' },
    });
    fireEvent.click(screen.getByTestId('delivery-supp-save'));
    await waitFor(() =>
      expect(createOpportunityArtifact).toHaveBeenCalledWith(
        7,
        expect.objectContaining({ type: 'SURVEY_REPORT', content: '现场走访纪要' }),
      ),
    );
    expect(createOpportunityArtifact).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ type: 'SURVEY_ATTACHMENT', link: 'https://x/site.jpg' }),
    );
    await waitFor(() => expect(advanceOpportunity).toHaveBeenCalledWith(7, undefined));
    expect(listOpportunities).toHaveBeenCalledTimes(2);
  });

  /** TC-DEL-09: 产品诉求（无门禁）「推进」直接 advance(id)，不开补充表单。 */
  it('advances a non-gated 实施 stage directly (TC-DEL-09)', async () => {
    vi.mocked(listOpportunities)
      .mockResolvedValueOnce(page([opp(7, 'REQUIREMENT')]))
      .mockResolvedValueOnce(page([opp(7, 'DELIVERY')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('delivery-advance-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('delivery-advance-7'));
    await waitFor(() => expect(advanceOpportunity).toHaveBeenCalledWith(7, undefined));
    expect(listOpportunityArtifacts).not.toHaveBeenCalled();
    expect(screen.queryByTestId('delivery-supp-SURVEY_REPORT')).not.toBeInTheDocument();
  });

  /** TC-DEL-03 / TC-FDH-01: 立项关联已有对外-交付项目（次要路径，先切到「关联已有」）— initiate({projectId}). */
  it('hands off to an existing 对外-交付 Project via initiate (TC-DEL-03 / TC-FDH-01)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'INITIATION')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('delivery-handoff-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('delivery-handoff-7'));
    // 关联 is the secondary path → switch to it first
    await waitFor(() => expect(screen.getByTestId('delivery-mode-link')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('delivery-mode-link'));
    await waitFor(() => expect(screen.getByTestId('delivery-project-select')).toBeInTheDocument());
    expect(listProjects).toHaveBeenCalledWith({ size: 100, projectType: 'EXTERNAL_DELIVERY' });
    fireEvent.change(screen.getByTestId('delivery-project-select'), { target: { value: '3' } });
    fireEvent.click(screen.getByTestId('delivery-handoff-save'));
    await waitFor(() =>
      expect(initiateOpportunity).toHaveBeenCalledWith(7, { decision: 'PASS', projectId: 3 }),
    );
  });

  /** TC-FDH-02: 立项新建对外-交付项目（默认路径，仅名称+负责人，编号自动生成）— initiate({projectName, owner}). */
  it('inline-creates an 对外-交付 Project via initiate (TC-FDH-02)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'INITIATION')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('delivery-handoff-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('delivery-handoff-7'));
    // create is the default mode — name field shown immediately; NO 编号 input (auto-generated)
    await waitFor(() => expect(screen.getByTestId('delivery-new-name')).toBeInTheDocument());
    expect(screen.queryByTestId('delivery-new-code')).not.toBeInTheDocument();
    fireEvent.change(screen.getByTestId('delivery-new-name'), { target: { value: '交付新项目' } });
    fireEvent.change(screen.getByTestId('delivery-new-owner'), { target: { value: '5' } });
    fireEvent.click(screen.getByTestId('delivery-handoff-save'));
    await waitFor(() =>
      expect(initiateOpportunity).toHaveBeenCalledWith(7, {
        decision: 'PASS',
        projectName: '交付新项目',
        projectOwnerUserId: 5,
      }),
    );
  });

  /** TC-FDH-04: 立项失败时展示后端 400 message（而非 axios 通用串）. */
  it('surfaces the backend 400 message on initiate failure (TC-FDH-04)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'INITIATION')]));
    vi.mocked(initiateOpportunity).mockRejectedValueOnce({
      response: { data: { message: '项目编号已存在: DLV-DUP' } },
      message: 'Request failed with status code 400',
    });
    renderPage();
    await waitFor(() => expect(screen.getByTestId('delivery-handoff-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('delivery-handoff-7'));
    await waitFor(() => expect(screen.getByTestId('delivery-new-name')).toBeInTheDocument());
    fireEvent.change(screen.getByTestId('delivery-new-name'), { target: { value: '重复' } });
    fireEvent.change(screen.getByTestId('delivery-new-owner'), { target: { value: '5' } });
    fireEvent.click(screen.getByTestId('delivery-handoff-save'));
    await waitFor(() =>
      expect(screen.getByTestId('delivery-handoff-error')).toHaveTextContent('项目编号已存在'),
    );
  });

  /** TC-FDH-03: 无对外-交付项目 → 抽屉直接进「新建」模式（智能默认，省去切换点击）即可提交. */
  it('opens directly in 新建 mode when no 对外-交付 projects (TC-FDH-03)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'INITIATION')]));
    vi.mocked(listProjects).mockResolvedValueOnce({ content: [], total: 0, page: 0, size: 100 });
    renderPage();
    await waitFor(() => expect(screen.getByTestId('delivery-handoff-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('delivery-handoff-7'));
    // smart default: nothing to link → create form shown directly, with NO extra mode-toggle click
    await waitFor(() => expect(screen.getByTestId('delivery-new-name')).toBeInTheDocument());
    expect(screen.queryByTestId('delivery-project-select')).not.toBeInTheDocument();
    fireEvent.change(screen.getByTestId('delivery-new-name'), { target: { value: '交付' } });
    fireEvent.change(screen.getByTestId('delivery-new-owner'), { target: { value: '6' } });
    fireEvent.click(screen.getByTestId('delivery-handoff-save'));
    await waitFor(() =>
      expect(initiateOpportunity).toHaveBeenCalledWith(7, {
        decision: 'PASS',
        projectName: '交付',
        projectOwnerUserId: 6,
      }),
    );
  });

  /** TC-FDH-01b: 即使已有对外-交付项目，抽屉仍默认进「新建」（create-first，不被项目数量埋没）. */
  it('defaults to 新建 mode even when 对外-交付 projects exist (TC-FDH-01b)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'INITIATION')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('delivery-handoff-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('delivery-handoff-7'));
    // create form shown by default; the link dropdown is only reached via the secondary toggle
    await waitFor(() => expect(screen.getByTestId('delivery-new-name')).toBeInTheDocument());
    expect(screen.queryByTestId('delivery-project-select')).not.toBeInTheDocument();
    // 关联 is available as a secondary one-click option
    fireEvent.click(screen.getByTestId('delivery-mode-link'));
    await waitFor(() => expect(screen.getByTestId('delivery-project-select')).toBeInTheDocument());
  });

  /** TC-DEL-NAV: 行「详情」跳转到统一详情页 /crm/opportunities/:id（不再开抽屉）。 */
  it('navigates to the unified detail page on 详情 (TC-DEL-NAV)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'SURVEY')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('delivery-detail-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('delivery-detail-7'));
    expect(mockNavigate).toHaveBeenCalledWith('/crm/opportunities/7');
    // 不再有内联详情抽屉
    expect(screen.queryByTestId('delivery-detail-body')).not.toBeInTheDocument();
  });
});
