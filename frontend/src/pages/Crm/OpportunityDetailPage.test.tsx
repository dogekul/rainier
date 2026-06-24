import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OpportunityDetailPage from './OpportunityDetailPage';
import {
  advanceOpportunity,
  getOpportunity,
  updateOpportunity,
  type Opportunity,
} from '../../api/opportunity';
import {
  createOpportunityArtifact,
  listOpportunityArtifacts,
  type OpportunityArtifact,
} from '../../api/opportunityArtifact';
import { listUsers } from '../../api/user';
import { listProducts } from '../../api/product';
import { listCustomers } from '../../api/customer';
import { createDemand, listDemands, type Demand } from '../../api/demand';
import { createRequirement, listRequirements, type Requirement } from '../../api/requirement';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (orig) => ({
  ...(await orig<typeof import('react-router-dom')>()),
  useNavigate: () => mockNavigate,
}));
// 当前登录用户（提供 numeric id 作 submitter/owner）
vi.mock('../../store/auth', () => ({
  useAuthStore: (selector: (s: { user: { id: number; username: string } }) => unknown) =>
    selector({ user: { id: 5, username: 'alice' } }),
}));
const emptyPage = { content: [], total: 0, page: 0, size: 100 };
vi.mock('../../api/demand', async (orig) => ({
  ...(await orig<typeof import('../../api/demand')>()),
  createDemand: vi.fn(() => Promise.resolve({} as Demand)),
  listDemands: vi.fn(() => Promise.resolve({ content: [], total: 0, page: 0, size: 100 })),
}));
vi.mock('../../api/requirement', async (orig) => ({
  ...(await orig<typeof import('../../api/requirement')>()),
  createRequirement: vi.fn(() => Promise.resolve({} as Requirement)),
  listRequirements: vi.fn(() => Promise.resolve({ content: [], total: 0, page: 0, size: 100 })),
}));
vi.mock('../../api/opportunity', async (orig) => ({
  ...(await orig<typeof import('../../api/opportunity')>()),
  getOpportunity: vi.fn(),
  updateOpportunity: vi.fn((id: number, body: Partial<Opportunity>) =>
    Promise.resolve({ id, stage: 'SURVEY', status: 'WON', ...body } as Opportunity),
  ),
  advanceOpportunity: vi.fn(() => Promise.resolve({} as Opportunity)),
}));
vi.mock('../../api/opportunityArtifact', async (orig) => ({
  ...(await orig<typeof import('../../api/opportunityArtifact')>()),
  listOpportunityArtifacts: vi.fn(() => Promise.resolve([] as OpportunityArtifact[])),
  createOpportunityArtifact: vi.fn(() => Promise.resolve({} as OpportunityArtifact)),
}));
vi.mock('../../api/user', () => ({
  listUsers: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 }),
}));
vi.mock('../../api/product', () => ({
  listProducts: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 }),
}));
vi.mock('../../api/customer', () => ({
  listCustomers: vi.fn().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 }),
}));

function oppOf(over: Partial<Opportunity> = {}): Opportunity {
  return {
    id: 7,
    customerName: 'X 集团',
    title: '采购系统',
    stage: 'SURVEY',
    status: 'WON',
    ...over,
  } as Opportunity;
}

function renderAt(id: number, search = '') {
  return render(
    <MemoryRouter initialEntries={[`/crm/opportunities/${id}${search}`]}>
      <Routes>
        <Route path="/crm/opportunities/:id" element={<OpportunityDetailPage />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('OpportunityDetailPage (统一商机详情页)', () => {
  beforeEach(() => {
    mockNavigate.mockClear();
    vi.mocked(getOpportunity).mockReset().mockResolvedValue(oppOf());
    vi.mocked(updateOpportunity).mockClear();
    vi.mocked(advanceOpportunity).mockClear();
    vi.mocked(listOpportunityArtifacts).mockReset().mockResolvedValue([] as OpportunityArtifact[]);
    vi.mocked(createOpportunityArtifact).mockClear();
    vi.mocked(listCustomers).mockClear().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 });
    vi.mocked(listProducts).mockClear();
    vi.mocked(listUsers).mockClear();
    vi.mocked(createDemand).mockClear();
    vi.mocked(createRequirement).mockClear();
    vi.mocked(listDemands).mockReset().mockResolvedValue(emptyPage);
    vi.mocked(listRequirements).mockReset().mockResolvedValue(emptyPage);
    // JSDOM 不实现 scrollIntoView —— 测试 ?action=convert 会调它，提供 stub。
    Element.prototype.scrollIntoView = vi.fn();
  });

  /** TC-ODP-01: 访问 /crm/opportunities/7 → fetch 商机 + 产出物，渲染概览。 */
  it('loads the opportunity and its artifacts by id (TC-ODP-01)', async () => {
    renderAt(7);
    await waitFor(() => expect(screen.getByTestId('opp-detail-page')).toBeInTheDocument());
    expect(getOpportunity).toHaveBeenCalledWith(7);
    expect(listOpportunityArtifacts).toHaveBeenCalledWith(7);
    await waitFor(() =>
      expect(screen.getByTestId('opp-detail-page')).toHaveTextContent('X 集团'),
    );
    expect(screen.getByTestId('opp-detail-page')).toHaveTextContent('现场调研'); // 阶段中文
  });

  /** TC-ODP-02: 产出物列表 — 报告类可导出、链接类可打开。 */
  it('lists artifacts with export (report) and link (TC-ODP-02)', async () => {
    vi.mocked(listOpportunityArtifacts).mockResolvedValue([
      { id: 91, opportunityId: 7, type: 'SURVEY_REPORT', typeLabel: '现场调研报告', title: '现场调研报告', content: '走访纪要' },
      { id: 92, opportunityId: 7, type: 'SURVEY_ATTACHMENT', typeLabel: '现场调研附件', title: '现场调研附件', link: 'https://x/site.jpg' },
    ] as OpportunityArtifact[]);
    renderAt(7);
    await waitFor(() => expect(screen.getByTestId('opp-detail-artifact-91')).toBeInTheDocument());
    expect(screen.getByTestId('opp-detail-export-91')).toBeInTheDocument();
    expect(screen.getByTestId('opp-detail-link-92')).toBeInTheDocument();
  });

  /** TC-ODP-03: 编辑标题并保存 → updateOpportunity 收到新值 + 按名解析 customerId。 */
  it('edits and saves, resolving customerId by name (TC-ODP-03)', async () => {
    vi.mocked(listCustomers).mockResolvedValue({
      content: [{ id: 42, name: 'X 集团' }],
      total: 1,
      page: 0,
      size: 100,
    } as Awaited<ReturnType<typeof listCustomers>>);
    renderAt(7);
    await waitFor(() => expect(screen.getByTestId('opp-detail-edit')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-detail-edit'));
    await waitFor(() => expect(screen.getByTestId('opp-detail-title')).toBeInTheDocument());
    fireEvent.change(screen.getByTestId('opp-detail-title'), { target: { value: '采购系统(改)' } });
    fireEvent.click(screen.getByTestId('opp-detail-save'));
    await waitFor(() =>
      expect(updateOpportunity).toHaveBeenCalledWith(
        7,
        expect.objectContaining({ title: '采购系统(改)', customerName: 'X 集团', customerId: 42 }),
      ),
    );
  });

  /** TC-ODP-04: 添加产出物（报告类）→ createOpportunityArtifact + 重拉列表。 */
  it('adds an artifact and reloads (TC-ODP-04)', async () => {
    renderAt(7);
    await waitFor(() => expect(screen.getByTestId('opp-detail-add-artifact')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-detail-add-artifact'));
    await waitFor(() => expect(screen.getByTestId('opp-detail-add-content')).toBeInTheDocument());
    fireEvent.change(screen.getByTestId('opp-detail-add-content'), { target: { value: '补充材料' } });
    fireEvent.click(screen.getByTestId('opp-detail-add-save'));
    await waitFor(() =>
      expect(createOpportunityArtifact).toHaveBeenCalledWith(
        7,
        expect.objectContaining({ type: 'SURVEY_REPORT', content: '补充材料' }),
      ),
    );
    expect(listOpportunityArtifacts).toHaveBeenCalledTimes(2); // initial + reload
  });

  /** TC-ODP-05: 加载失败 → 错误态。 */
  it('shows an error state when the fetch fails (TC-ODP-05)', async () => {
    vi.mocked(getOpportunity).mockRejectedValue(new Error('not found'));
    renderAt(999);
    await waitFor(() => expect(screen.getByTestId('opp-detail-error')).toBeInTheDocument());
    expect(screen.queryByTestId('opp-detail-edit')).not.toBeInTheDocument();
  });

  /** TC-ODP-06: 返回 → navigate(-1)。 */
  it('navigates back on 返回 (TC-ODP-06)', async () => {
    renderAt(7);
    await waitFor(() => expect(screen.getByTestId('opp-detail-back')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-detail-back'));
    expect(mockNavigate).toHaveBeenCalledWith(-1);
  });

  /** TC-ODP-07: 添加链接类产出物 → 提交 link（非 content）。 */
  it('adds a link-kind artifact with link, not content (TC-ODP-07)', async () => {
    renderAt(7);
    await waitFor(() => expect(screen.getByTestId('opp-detail-add-artifact')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-detail-add-artifact'));
    await waitFor(() => expect(screen.getByTestId('opp-detail-add-type')).toBeInTheDocument());
    fireEvent.change(screen.getByTestId('opp-detail-add-type'), { target: { value: 'SURVEY_ATTACHMENT' } });
    await waitFor(() => expect(screen.getByTestId('opp-detail-add-link')).toBeInTheDocument());
    expect(screen.queryByTestId('opp-detail-add-content')).not.toBeInTheDocument(); // 链接类无正文
    fireEvent.change(screen.getByTestId('opp-detail-add-link'), { target: { value: 'https://x/a.jpg' } });
    fireEvent.click(screen.getByTestId('opp-detail-add-save'));
    await waitFor(() =>
      expect(createOpportunityArtifact).toHaveBeenCalledWith(
        7,
        expect.objectContaining({ type: 'SURVEY_ATTACHMENT', link: 'https://x/a.jpg' }),
      ),
    );
  });

  /** TC-ODP-08: 非法 id → 错误态，不调 getOpportunity。 */
  it('shows error for a non-numeric id without fetching (TC-ODP-08)', async () => {
    render(
      <MemoryRouter initialEntries={['/crm/opportunities/abc']}>
        <Routes>
          <Route path="/crm/opportunities/:id" element={<OpportunityDetailPage />} />
        </Routes>
      </MemoryRouter>,
    );
    await waitFor(() => expect(screen.getByTestId('opp-detail-error')).toBeInTheDocument());
    expect(getOpportunity).not.toHaveBeenCalled();
  });

  /** TC-OGEN-F1: 点「生成」打开草稿，描述据 现场调研+产品 预填。 */
  it('opens a draft prefilled from survey + product (TC-OGEN-F1)', async () => {
    vi.mocked(getOpportunity).mockResolvedValue(oppOf({ productName: '采购平台' }));
    vi.mocked(listOpportunityArtifacts).mockResolvedValue([
      { id: 1, opportunityId: 7, type: 'SURVEY_REPORT', typeLabel: '现场调研报告', title: '报告', content: '调研结论X' },
    ] as OpportunityArtifact[]);
    renderAt(7);
    await waitFor(() => expect(screen.getByTestId('opp-gen-open')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-gen-open'));
    await waitFor(() => expect(screen.getByTestId('opp-gen-form')).toBeInTheDocument());
    const desc = screen.getByTestId('opp-gen-desc') as HTMLTextAreaElement;
    expect(desc.value).toContain('调研结论X');
    expect(desc.value).toContain('采购平台');
  });

  /** TC-OGEN-F2: 目标诉求 → 诉求字段(来源)，提交 createDemand 带 source+opportunityId，且不含需求专属字段。 */
  it('submits a Demand with source (not requirement fields) (TC-OGEN-F2)', async () => {
    renderAt(7);
    await waitFor(() => expect(screen.getByTestId('opp-gen-open')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-gen-open'));
    await waitFor(() => expect(screen.getByTestId('opp-gen-title')).toBeInTheDocument());
    // 诉求表单：有「来源」，无 需求专属字段（复杂度/期望日期/负责人）；编号永不在表单（后端自增）
    expect(screen.getByTestId('opp-gen-source')).toBeInTheDocument();
    expect(screen.queryByTestId('opp-gen-code')).not.toBeInTheDocument();
    expect(screen.queryByTestId('opp-gen-complexity')).not.toBeInTheDocument();
    expect(screen.queryByTestId('opp-gen-owner')).not.toBeInTheDocument();
    fireEvent.change(screen.getByTestId('opp-gen-title'), { target: { value: '采购诉求A' } });
    fireEvent.change(screen.getByTestId('opp-gen-source'), { target: { value: 'WECHAT' } });
    fireEvent.click(screen.getByTestId('opp-gen-save'));
    await waitFor(() =>
      expect(createDemand).toHaveBeenCalledWith(
        expect.objectContaining({
          title: '采购诉求A',
          source: 'WECHAT',
          opportunityId: 7,
          submitterUserId: 5,
        }),
      ),
    );
  });

  /** TC-OGEN-F3: 切换需求 → 需求字段(负责人(PO)/复杂度/期望日期)，无编号输入（后端自增 REQ-{id}）；提交 createRequirement 不含 code、带 owner+opportunityId。 */
  it('submits a Requirement with owner/complexity, no code (TC-OGEN-F3)', async () => {
    renderAt(7);
    await waitFor(() => expect(screen.getByTestId('opp-gen-open')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-gen-open'));
    await waitFor(() => expect(screen.getByTestId('opp-gen-target-requirement')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-gen-target-requirement'));
    // 需求表单：有「负责人/复杂度/期望日期」，无「编号/来源」
    await waitFor(() => expect(screen.getByTestId('opp-gen-owner')).toBeInTheDocument());
    expect(screen.getByTestId('opp-gen-complexity')).toBeInTheDocument();
    expect(screen.getByTestId('opp-gen-expected')).toBeInTheDocument();
    expect(screen.queryByTestId('opp-gen-code')).not.toBeInTheDocument();
    expect(screen.queryByTestId('opp-gen-source')).not.toBeInTheDocument();
    fireEvent.change(screen.getByTestId('opp-gen-complexity'), { target: { value: 'M' } });
    fireEvent.click(screen.getByTestId('opp-gen-save'));
    await waitFor(() => expect(createRequirement).toHaveBeenCalled());
    const body = vi.mocked(createRequirement).mock.calls[0][0];
    expect(body.complexity).toBe('M');
    expect(body.opportunityId).toBe(7);
    expect(body.ownerUserId).toBe(5); // 默认 = 当前登录用户
    expect(body.code).toBeUndefined(); // v0.0.56 — 前端不再发送 code
    expect(createDemand).not.toHaveBeenCalled();
  });

  /** TC-OGEN-F4: 已生成区列出本商机派生的诉求/需求。 */
  it('lists already-generated demands and requirements (TC-OGEN-F4)', async () => {
    vi.mocked(listDemands).mockResolvedValue({
      content: [{ id: 11, title: '诉求一', status: 'PENDING' }],
      total: 1,
      page: 0,
      size: 100,
    } as Awaited<ReturnType<typeof listDemands>>);
    vi.mocked(listRequirements).mockResolvedValue({
      content: [{ id: 22, code: 'OPP-7-x', title: '需求一', status: 'DRAFT' }],
      total: 1,
      page: 0,
      size: 100,
    } as Awaited<ReturnType<typeof listRequirements>>);
    renderAt(7);
    await waitFor(() => expect(screen.getByTestId('opp-gen-list')).toBeInTheDocument());
    await waitFor(() => expect(screen.getByTestId('opp-gen-demand-11')).toBeInTheDocument());
    expect(screen.getByTestId('opp-gen-requirement-22')).toBeInTheDocument();
    expect(listDemands).toHaveBeenCalledWith(expect.objectContaining({ opportunityId: 7 }));
    expect(listRequirements).toHaveBeenCalledWith(expect.objectContaining({ opportunityId: 7 }));
  });

  /** TC-OGEN-F5: 入参 ?action=convert → 展示补全提示横幅 + 滚动到生成卡。 */
  it('shows the convert prompt + scrolls to the gen card when arriving with ?action=convert (TC-OGEN-F5)', async () => {
    const scrollSpy = vi.spyOn(Element.prototype, 'scrollIntoView');
    renderAt(7, '?action=convert');
    await waitFor(() =>
      expect(screen.getByTestId('opp-gen-convert-prompt')).toBeInTheDocument(),
    );
    // scrollIntoView 由生成卡触发，args 含 {behavior:'smooth', block:'start'}
    expect(scrollSpy).toHaveBeenCalled();
    const callsOnGenCard = scrollSpy.mock.instances.some(
      (el) => (el as unknown as HTMLElement).dataset?.testid === 'opp-gen-card',
    );
    expect(callsOnGenCard).toBe(true);
  });

  /** TC-OGEN-F6: 入参不含 action=convert → 不显示提示横幅。 */
  it('does not show the convert prompt without ?action=convert (TC-OGEN-F6)', async () => {
    renderAt(7);
    await waitFor(() => expect(screen.getByTestId('opp-gen-card')).toBeInTheDocument());
    expect(screen.queryByTestId('opp-gen-convert-prompt')).not.toBeInTheDocument();
  });

  /** TC-OGEN-F7: 从 ?action=convert 进入，提交需求后自动 advance 成功 → 关闭提示横幅 + 刷新商机。 */
  it('auto-advances and dismisses the prompt after submitting a Requirement (TC-OGEN-F7)', async () => {
    vi.mocked(getOpportunity)
      .mockResolvedValueOnce(oppOf({ stage: 'REQUIREMENT' }))
      .mockResolvedValueOnce(oppOf({ stage: 'DELIVERY' }));
    renderAt(7, '?action=convert');
    await waitFor(() => expect(screen.getByTestId('opp-gen-convert-prompt')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-gen-open'));
    await waitFor(() => expect(screen.getByTestId('opp-gen-target-requirement')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-gen-target-requirement'));
    await waitFor(() => expect(screen.getByTestId('opp-gen-save')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-gen-save'));
    await waitFor(() => expect(createRequirement).toHaveBeenCalled());
    // 自动 advance 被调用 + 提示横幅关闭 + 商机被重新拉取
    await waitFor(() => expect(advanceOpportunity).toHaveBeenCalledWith(7));
    await waitFor(() =>
      expect(screen.queryByTestId('opp-gen-convert-prompt')).not.toBeInTheDocument(),
    );
    expect(getOpportunity).toHaveBeenCalledTimes(2); // 初始 + advance 后刷新
  });

  /** TC-OGEN-F8: 提交诉求但后端 advance 拒绝（无需求）→ 横幅保留并显示后端 message（红色）。 */
  it('shows the backend error in the prompt when auto-advance fails (TC-OGEN-F8)', async () => {
    vi.mocked(advanceOpportunity).mockRejectedValueOnce({
      response: { data: { message: '请先将诉求转化为需求' } },
    });
    renderAt(7, '?action=convert');
    await waitFor(() => expect(screen.getByTestId('opp-gen-convert-prompt')).toBeInTheDocument());
    fireEvent.click(screen.getByTestId('opp-gen-open'));
    await waitFor(() => expect(screen.getByTestId('opp-gen-title')).toBeInTheDocument());
    // 默认 target=demand；提交诉求
    fireEvent.click(screen.getByTestId('opp-gen-save'));
    await waitFor(() => expect(createDemand).toHaveBeenCalled());
    await waitFor(() => expect(advanceOpportunity).toHaveBeenCalledWith(7));
    // 横幅仍在，且显示后端 message
    await waitFor(() =>
      expect(screen.getByTestId('opp-gen-convert-prompt-text')).toHaveTextContent(
        '请先将诉求转化为需求',
      ),
    );
  });
});
