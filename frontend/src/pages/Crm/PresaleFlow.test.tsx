import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { PresaleFlow } from './PresaleFlow';
import {
  advanceOpportunity,
  createOpportunity,
  updateOpportunity,
  listOpportunities,
  type Opportunity,
} from '../../api/opportunity';
import {
  createOpportunityArtifact,
  exportArtifactDocx,
  listOpportunityArtifacts,
} from '../../api/opportunityArtifact';

vi.mock('../../api/opportunity', async (orig) => ({
  ...(await orig<typeof import('../../api/opportunity')>()),
  listOpportunities: vi.fn(),
  advanceOpportunity: vi.fn(() => Promise.resolve({} as Opportunity)),
  createOpportunity: vi.fn(() => Promise.resolve({} as Opportunity)),
  updateOpportunity: vi.fn(() => Promise.resolve({} as Opportunity)),
}));
vi.mock('../../api/user', () => ({
  listUsers: vi.fn().mockResolvedValue({
    content: [{ id: 5, loginName: 'li', name: '李商务', isInternal: true, enabled: true }],
    total: 1,
    page: 0,
    size: 100,
  }),
}));
vi.mock('../../api/product', () => ({
  listProducts: vi.fn().mockResolvedValue({
    content: [{ id: 9, code: 'PRD', name: '采购平台', status: 'ACTIVE' }],
    total: 1,
    page: 0,
    size: 100,
  }),
}));
vi.mock('../../api/customer', () => ({
  listCustomers: vi.fn().mockResolvedValue({
    content: [{ id: 3, name: '老客户' }],
    total: 1,
    page: 0,
    size: 100,
  }),
}));
vi.mock('../../api/opportunityArtifact', async (orig) => ({
  ...(await orig<typeof import('../../api/opportunityArtifact')>()),
  listOpportunityArtifacts: vi.fn(() => Promise.resolve([])),
  exportArtifactDocx: vi.fn(() => Promise.resolve()),
  createOpportunityArtifact: vi.fn(() => Promise.resolve({ id: 1 } as never)),
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

function fillArtifact(title: string, content: string) {
  fireEvent.change(screen.getByTestId('presale-artifact-title'), { target: { value: title } });
  fireEvent.change(screen.getByTestId('presale-artifact-content'), { target: { value: content } });
}

describe('PresaleFlow (售前流转 operations)', () => {
  beforeEach(() => {
    vi.mocked(advanceOpportunity).mockReset();
    vi.mocked(advanceOpportunity).mockResolvedValue({} as Opportunity);
    vi.mocked(createOpportunity).mockClear();
    vi.mocked(updateOpportunity).mockClear();
    vi.mocked(createOpportunityArtifact).mockClear();
    vi.mocked(exportArtifactDocx).mockClear();
    vi.mocked(listOpportunityArtifacts).mockReset();
    vi.mocked(listOpportunityArtifacts).mockResolvedValue([]);
    vi.mocked(listOpportunities).mockReset();
  });

  /** TC-POC-01: POC 推进 opens the「补充产出物」form for the missing required docs (link vs report inputs). */
  it('POC 推进 opens the supplement form for missing docs (TC-POC-01)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'POC')]));
    vi.mocked(listOpportunityArtifacts).mockResolvedValue([]); // none present → all 4 missing
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-advance-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-advance-7'));
    await waitFor(() =>
      expect(screen.getByTestId('presale-supp-PRESENTATION_MATERIAL')).toBeInTheDocument(),
    );
    expect(screen.getByTestId('presale-supp-CLIENT_REQUIREMENTS')).toBeInTheDocument();
    expect(screen.getByTestId('presale-supp-POC_SCORE')).toBeInTheDocument();
    expect(screen.getByTestId('presale-supp-GAP_ANALYSIS')).toBeInTheDocument();
    // link kind → indexed link input + 添加链接 button, NO title input; report kind → content textarea
    expect(screen.getByTestId('presale-supp-link-PRESENTATION_MATERIAL-0')).toBeInTheDocument();
    expect(screen.getByTestId('presale-supp-addlink-PRESENTATION_MATERIAL')).toBeInTheDocument();
    expect(screen.queryByTestId('presale-supp-title-PRESENTATION_MATERIAL')).not.toBeInTheDocument();
    expect(screen.getByTestId('presale-supp-content-POC_SCORE')).toBeInTheDocument();
    expect(advanceOpportunity).not.toHaveBeenCalled();
  });

  /** TC-POC-02: supplement form — link kinds take multiple links (no title), reports take content; then advances. */
  it('supplement form creates missing docs (multi-link, no title) then advances (TC-POC-02)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'POC')]));
    vi.mocked(listOpportunityArtifacts).mockResolvedValue([]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-advance-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-advance-7'));
    await waitFor(() => expect(screen.getByTestId('presale-supp-save')).toBeInTheDocument());
    // 讲解材料：可添加多份链接，无需标题
    fireEvent.change(screen.getByTestId('presale-supp-link-PRESENTATION_MATERIAL-0'), {
      target: { value: 'https://x/ppt1' },
    });
    fireEvent.click(screen.getByTestId('presale-supp-addlink-PRESENTATION_MATERIAL'));
    await waitFor(() =>
      expect(screen.getByTestId('presale-supp-link-PRESENTATION_MATERIAL-1')).toBeInTheDocument(),
    );
    fireEvent.change(screen.getByTestId('presale-supp-link-PRESENTATION_MATERIAL-1'), {
      target: { value: 'https://x/ppt2' },
    });
    fireEvent.change(screen.getByTestId('presale-supp-link-CLIENT_REQUIREMENTS-0'), {
      target: { value: 'https://x/req' },
    });
    fireEvent.change(screen.getByTestId('presale-supp-content-POC_SCORE'), { target: { value: '90' } });
    fireEvent.change(screen.getByTestId('presale-supp-content-GAP_ANALYSIS'), {
      target: { value: '集成' },
    });
    fireEvent.click(screen.getByTestId('presale-supp-save'));
    // 2 讲解材料 links + 1 诉求清单 + 1 得分表 + 1 差距分析 = 5 artifacts
    await waitFor(() => expect(createOpportunityArtifact).toHaveBeenCalledTimes(5));
    expect(createOpportunityArtifact).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ type: 'PRESENTATION_MATERIAL', link: 'https://x/ppt1' }),
    );
    expect(createOpportunityArtifact).toHaveBeenCalledWith(
      7,
      expect.objectContaining({ type: 'PRESENTATION_MATERIAL', link: 'https://x/ppt2' }),
    );
    // link-kind artifacts carry no title
    expect(createOpportunityArtifact).not.toHaveBeenCalledWith(
      7,
      expect.objectContaining({ type: 'PRESENTATION_MATERIAL', title: expect.anything() }),
    );
    await waitFor(() =>
      expect(advanceOpportunity).toHaveBeenCalledWith(7, undefined, undefined, undefined),
    );
  });

  /** TC-PAR-04: 详情「添加产出物」of a link kind submits link, not content. */
  it('adds a link-kind artifact from 详情 (TC-PAR-04)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'POC')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-detail-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-detail-7'));
    await waitFor(() => expect(screen.getByTestId('presale-detail-add-artifact')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-detail-add-artifact'));
    await waitFor(() => expect(screen.getByTestId('presale-add-type')).toBeInTheDocument());
    // PRESENTATION_MATERIAL is link-kind → link input shown, NO title nor content
    expect(screen.getByTestId('presale-add-link')).toBeInTheDocument();
    expect(screen.queryByTestId('presale-add-content')).not.toBeInTheDocument();
    expect(screen.queryByTestId('presale-add-title')).not.toBeInTheDocument();
    fireEvent.change(screen.getByTestId('presale-add-link'), { target: { value: 'https://x/ppt' } });
    fireEvent.click(screen.getByTestId('presale-add-save'));
    await waitFor(() =>
      expect(createOpportunityArtifact).toHaveBeenCalledWith(
        7,
        expect.objectContaining({ type: 'PRESENTATION_MATERIAL', link: 'https://x/ppt' }),
      ),
    );
    // link-kind carries no title
    expect(createOpportunityArtifact).not.toHaveBeenCalledWith(
      7,
      expect.objectContaining({ title: expect.anything() }),
    );
  });

  /** TC-PDE-01: 详情 defaults to READ-ONLY (编辑 button, no inputs) + 产出物 rich-text 预览. */
  it('详情 is read-only by default with an artifact rich preview (TC-PDE-01)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(
      page([opp(7, 'OPPORTUNITY', { note: '关键客户备注', amount: 200000 })]),
    );
    vi.mocked(listOpportunityArtifacts).mockResolvedValue([
      {
        id: 55,
        opportunityId: 7,
        type: 'RESEARCH_REPORT',
        typeLabel: '商机调研报告',
        stageFrom: 'LEAD',
        title: '调研',
        content: '# 结论\n\n**通过**',
        author: 'alice',
      },
    ]);
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-detail-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-detail-7'));
    await waitFor(() => expect(screen.getByTestId('presale-detail-body')).toBeInTheDocument());
    // read-only by default: 编辑 button shown, edit inputs absent, 备注 shown as text
    expect(screen.getByTestId('presale-detail-edit')).toBeInTheDocument();
    expect(screen.queryByTestId('presale-detail-title')).not.toBeInTheDocument();
    expect(screen.getByText('关键客户备注')).toBeInTheDocument();
    // artifact rich-text 预览
    await waitFor(() => expect(screen.getByTestId('presale-detail-artifact-55')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-detail-preview-55'));
    await waitFor(() => expect(screen.getByTestId('presale-detail-md-55')).toBeInTheDocument());
    expect(screen.getByTestId('presale-detail-md-55').querySelector('strong')?.textContent).toBe(
      '通过',
    );
    fireEvent.click(screen.getByTestId('presale-detail-export-55'));
    await waitFor(() =>
      expect(exportArtifactDocx).toHaveBeenCalledWith(7, 55, expect.stringContaining('.docx')),
    );
  });

  /** TC-PDE-02: 编辑 button reveals the form; editing + 保存修改 calls updateOpportunity. */
  it('edits an opportunity after clicking 编辑 in 详情 (TC-PDE-02)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'OPPORTUNITY')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-detail-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-detail-7'));
    await waitFor(() => expect(screen.getByTestId('presale-detail-edit')).toBeInTheDocument());
    // no inputs until 编辑 clicked
    expect(screen.queryByTestId('presale-detail-title')).not.toBeInTheDocument();
    fireEvent.click(screen.getByTestId('presale-detail-edit'));
    await waitFor(() => expect(screen.getByTestId('presale-detail-title')).toBeInTheDocument());
    expect(screen.getByTestId('presale-detail-title')).toHaveValue('采购系统'); // prefilled
    fireEvent.change(screen.getByTestId('presale-detail-title'), { target: { value: '采购系统 V2' } });
    fireEvent.change(screen.getByTestId('presale-detail-note'), { target: { value: '改过的备注' } });
    fireEvent.click(screen.getByTestId('presale-detail-save'));
    await waitFor(() =>
      expect(updateOpportunity).toHaveBeenCalledWith(
        7,
        expect.objectContaining({ title: '采购系统 V2', note: '改过的备注' }),
      ),
    );
  });

  /** TC-PDE-03: advancing from 详情 closes it and routes through the gate (商机→纪要 form). */
  it('advances from the 详情 drawer (TC-PDE-03)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'OPPORTUNITY')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-detail-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-detail-7'));
    await waitFor(() => expect(screen.getByTestId('presale-detail-pass')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-detail-pass'));
    // OPPORTUNITY is artifact-gated → detail closes, the 决策评审纪要 form opens
    await waitFor(() => expect(screen.queryByTestId('presale-detail-body')).not.toBeInTheDocument());
    await waitFor(() => expect(screen.getByTestId('presale-artifact-title')).toBeInTheDocument());
    fillArtifact('评审', '通过');
    fireEvent.click(screen.getByTestId('presale-artifact-save'));
    await waitFor(() =>
      expect(advanceOpportunity).toHaveBeenCalledWith(7, 'PASS', undefined, {
        title: '评审',
        content: '通过',
      }),
    );
  });

  /** TC-PRE-01: lists only OPEN 售前 opps; gate rows show 通过/否决, non-gate show 推进; WON/LOST/实施 filtered out. */
  it('lists OPEN 售前 opps with stage-appropriate actions (TC-PRE-01)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(
      page([
        opp(7, 'OPPORTUNITY'), // gate
        opp(8, 'LEAD'), // non-gate
        opp(9, 'INITIATION', { status: 'WON' }), // 实施 (WON) → excluded
        opp(10, 'BIDDING', { status: 'LOST' }), // lost → excluded
        opp(11, 'SURVEY', { status: 'OPEN' }), // OPEN but 实施 stage → excluded by STAGE filter
      ]),
    );
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-row-7')).toBeInTheDocument());
    expect(listOpportunities).toHaveBeenCalledWith({ size: 100 });
    expect(screen.getByTestId('presale-pass-7')).toBeInTheDocument();
    expect(screen.getByTestId('presale-reject-7')).toBeInTheDocument();
    expect(screen.getByTestId('presale-advance-8')).toBeInTheDocument();
    expect(screen.queryByTestId('presale-row-9')).not.toBeInTheDocument();
    expect(screen.queryByTestId('presale-row-10')).not.toBeInTheDocument();
    expect(screen.queryByTestId('presale-row-11')).not.toBeInTheDocument();
  });

  /** TC-PRE-02: 商机通过 opens the 《决策评审纪要》 form; submit advances with PASS + artifact (v0.0.45 gating). */
  it('商机通过 requires 决策评审纪要 then advances PASS (TC-PRE-02)', async () => {
    vi.mocked(listOpportunities)
      .mockResolvedValueOnce(page([opp(7, 'OPPORTUNITY')]))
      .mockResolvedValueOnce(page([opp(7, 'POC')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-pass-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-pass-7'));
    // artifact form opens — does NOT advance yet
    await waitFor(() => expect(screen.getByTestId('presale-artifact-title')).toBeInTheDocument());
    expect(advanceOpportunity).not.toHaveBeenCalled();
    fillArtifact('评审纪要', '通过理由…');
    fireEvent.click(screen.getByTestId('presale-artifact-save'));
    await waitFor(() =>
      expect(advanceOpportunity).toHaveBeenCalledWith(7, 'PASS', undefined, {
        title: '评审纪要',
        content: '通过理由…',
      }),
    );
    expect(listOpportunities).toHaveBeenCalledTimes(2);
  });

  /** TC-PRE-03: 新建商机 drawer renders the 4 owner selects + creates with all filled fields. */
  it('creates an opportunity with an owner via the drawer (TC-PRE-03)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-empty')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-new-btn'));
    await waitFor(() => expect(screen.getByTestId('presale-new-customer')).toBeInTheDocument());
    expect(screen.getByTestId('presale-owner-commercial')).toBeInTheDocument();
    expect(screen.getByTestId('presale-owner-solution')).toBeInTheDocument();
    expect(screen.getByTestId('presale-owner-pm')).toBeInTheDocument();
    expect(screen.getByTestId('presale-owner-ops')).toBeInTheDocument();
    // v0.0.45 — 产品标签 dropdown (optional)
    expect(screen.getByTestId('presale-new-product')).toBeInTheDocument();
    fireEvent.change(screen.getByTestId('presale-new-customer'), { target: { value: '远方集团' } });
    fireEvent.change(screen.getByTestId('presale-new-title'), { target: { value: '对外交付' } });
    fireEvent.change(screen.getByTestId('presale-new-note'), { target: { value: '高优先级客户' } });
    fireEvent.change(screen.getByTestId('presale-new-amount'), { target: { value: '500000' } });
    await waitFor(() =>
      expect(screen.getByTestId('presale-owner-commercial')).toHaveTextContent('李商务'),
    );
    fireEvent.change(screen.getByTestId('presale-owner-commercial'), { target: { value: '5' } });
    await waitFor(() =>
      expect(screen.getByTestId('presale-new-product')).toHaveTextContent('采购平台'),
    );
    fireEvent.change(screen.getByTestId('presale-new-product'), { target: { value: '9' } });
    fireEvent.click(screen.getByTestId('presale-save-btn'));
    await waitFor(() =>
      expect(createOpportunity).toHaveBeenCalledWith(
        expect.objectContaining({
          customerName: '远方集团',
          title: '对外交付',
          note: '高优先级客户',
          amount: 500000,
          commercialOwnerUserId: 5,
          productId: 9,
        }),
      ),
    );
  });

  /** TC-PRE-04: 新建 empty required fields → form error, no create. */
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

  /** TC-PCU-01: typing an existing customer name resolves its customerId on create. */
  it('resolves an existing customer by name on create (TC-PCU-01)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-empty')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-new-btn'));
    await waitFor(() => expect(screen.getByTestId('presale-new-customer')).toBeInTheDocument());
    fireEvent.change(screen.getByTestId('presale-new-customer'), { target: { value: '老客户' } });
    fireEvent.change(screen.getByTestId('presale-new-title'), { target: { value: '复购' } });
    fireEvent.click(screen.getByTestId('presale-save-btn'));
    await waitFor(() =>
      expect(createOpportunity).toHaveBeenCalledWith(
        expect.objectContaining({ customerName: '老客户', customerId: 3 }),
      ),
    );
  });

  /** TC-PAR-01: 线索推进 opens the 《商机调研报告》 form; submit advances with NO decision + artifact. */
  it('线索推进 requires 商机调研报告 then advances (TC-PAR-01)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(8, 'LEAD')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-advance-8')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-advance-8'));
    await waitFor(() => expect(screen.getByTestId('presale-artifact-title')).toBeInTheDocument());
    expect(advanceOpportunity).not.toHaveBeenCalled();
    fillArtifact('调研报告', '客户背景…');
    fireEvent.click(screen.getByTestId('presale-artifact-save'));
    await waitFor(() =>
      expect(advanceOpportunity).toHaveBeenCalledWith(8, undefined, undefined, {
        title: '调研报告',
        content: '客户背景…',
      }),
    );
  });

  /** TC-PAR-02: 商机否决 opens the 纪要 form (records REJECT); submit advances with REJECT + artifact. */
  it('商机否决 requires 决策评审纪要 then advances REJECT (TC-PAR-02)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(7, 'OPPORTUNITY')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-reject-7')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-reject-7'));
    await waitFor(() => expect(screen.getByTestId('presale-artifact-title')).toBeInTheDocument());
    fillArtifact('评审纪要', '预算不足，否决');
    fireEvent.click(screen.getByTestId('presale-artifact-save'));
    await waitFor(() =>
      expect(advanceOpportunity).toHaveBeenCalledWith(7, 'REJECT', undefined, {
        title: '评审纪要',
        content: '预算不足，否决',
      }),
    );
  });

  /** TC-PAR-03: 产出物 form with empty fields → error, no advance. */
  it('artifact form blocks submit when empty (TC-PAR-03)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(8, 'LEAD')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-advance-8')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-advance-8'));
    await waitFor(() => expect(screen.getByTestId('presale-artifact-save')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-artifact-save'));
    await waitFor(() => expect(screen.getByTestId('presale-artifact-error')).toBeInTheDocument());
    expect(advanceOpportunity).not.toHaveBeenCalled();
  });

  /** TC-PRE-05: a non-artifact gate (投标) 否决 still confirms first, then advances REJECT. */
  it('non-artifact gate 否决 confirms then rejects (TC-PRE-05)', async () => {
    vi.mocked(listOpportunities).mockResolvedValue(page([opp(9, 'BIDDING')]));
    renderPage();
    await waitFor(() => expect(screen.getByTestId('presale-reject-9')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('presale-reject-9'));
    // 投标 has no artifact rule → ConfirmDialog (not the artifact form)
    await waitFor(() => expect(screen.getByTestId('rainier-confirm')).toBeInTheDocument());
    expect(screen.queryByTestId('presale-artifact-title')).not.toBeInTheDocument();
    expect(advanceOpportunity).not.toHaveBeenCalled();
    fireEvent.click(screen.getByText('确认否决'));
    await waitFor(() =>
      expect(advanceOpportunity).toHaveBeenCalledWith(9, 'REJECT', undefined, undefined),
    );
  });
});
