import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import OpportunityDetailPage from './OpportunityDetailPage';
import { getOpportunity, updateOpportunity, type Opportunity } from '../../api/opportunity';
import {
  createOpportunityArtifact,
  listOpportunityArtifacts,
  type OpportunityArtifact,
} from '../../api/opportunityArtifact';
import { listUsers } from '../../api/user';
import { listProducts } from '../../api/product';
import { listCustomers } from '../../api/customer';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (orig) => ({
  ...(await orig<typeof import('react-router-dom')>()),
  useNavigate: () => mockNavigate,
}));
vi.mock('../../api/opportunity', async (orig) => ({
  ...(await orig<typeof import('../../api/opportunity')>()),
  getOpportunity: vi.fn(),
  updateOpportunity: vi.fn((id: number, body: Partial<Opportunity>) =>
    Promise.resolve({ id, stage: 'SURVEY', status: 'WON', ...body } as Opportunity),
  ),
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

function renderAt(id: number) {
  return render(
    <MemoryRouter initialEntries={[`/crm/opportunities/${id}`]}>
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
    vi.mocked(listOpportunityArtifacts).mockReset().mockResolvedValue([] as OpportunityArtifact[]);
    vi.mocked(createOpportunityArtifact).mockClear();
    vi.mocked(listCustomers).mockClear().mockResolvedValue({ content: [], total: 0, page: 0, size: 100 });
    vi.mocked(listProducts).mockClear();
    vi.mocked(listUsers).mockClear();
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
});
