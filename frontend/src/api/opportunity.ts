import client from './client';
import type { PaginatedResult } from '../hooks/usePaginated';

export type OpportunityStage =
  | 'LEAD'
  | 'OPPORTUNITY'
  | 'POC'
  | 'BIDDING'
  | 'CONTRACT'
  | 'INITIATION'
  | 'SURVEY'
  | 'REQUIREMENT'
  | 'DELIVERY'
  | 'ACCEPTANCE';
export type OpportunityStatus = 'OPEN' | 'WON' | 'LOST';
export type GateDecision = 'PASS' | 'REJECT';

/** Full customer-journey order: 售前 5 节点 + 实施 5 节点. */
export const OPP_STAGE_ORDER: OpportunityStage[] = [
  'LEAD',
  'OPPORTUNITY',
  'POC',
  'BIDDING',
  'CONTRACT',
  'INITIATION',
  'SURVEY',
  'REQUIREMENT',
  'DELIVERY',
  'ACCEPTANCE',
];
export const OPP_STAGE_LABELS: Record<OpportunityStage, string> = {
  LEAD: '线索',
  OPPORTUNITY: '商机',
  POC: '推介/POC',
  BIDDING: '投标',
  CONTRACT: '合同签订',
  INITIATION: '立项',
  SURVEY: '现场调研',
  REQUIREMENT: '产品诉求',
  DELIVERY: '交付实施',
  ACCEPTANCE: '验收',
};
/** Decision gates (商机决策 / 投标决策 / 合同评审 / 立项评审). */
export const OPP_GATE_STAGES: OpportunityStage[] = ['OPPORTUNITY', 'BIDDING', 'CONTRACT', 'INITIATION'];

/** Phase bands matching the 客户全流程 diagram. 售前 = LEAD..CONTRACT, 实施 = INITIATION..ACCEPTANCE. */
export const OPP_PHASES: { key: string; label: string; owner: string; stages: OpportunityStage[] }[] = [
  {
    key: 'presale',
    label: '售前环节',
    owner: '商务 / 解决方案',
    stages: ['LEAD', 'OPPORTUNITY', 'POC', 'BIDDING', 'CONTRACT'],
  },
  {
    key: 'delivery',
    label: '实施环节',
    owner: '项目经理',
    stages: ['INITIATION', 'SURVEY', 'REQUIREMENT', 'DELIVERY', 'ACCEPTANCE'],
  },
];

/** 售前环节 stages (线索..合同签订) — drives the「售前流转」operation page. */
export const OPP_PRESALE_STAGES: OpportunityStage[] = OPP_PHASES[0].stages;
/** 实施环节 stages (立项..验收) — drives the「实施流转」operation page. */
export const OPP_DELIVERY_STAGES: OpportunityStage[] = OPP_PHASES[1].stages;

/**
 * v0.0.45 — transitions that require a 流转产出物 before advancing (mirrors backend TransitionArtifactRules,
 * keyed by SOURCE stage). 线索→商机 needs《商机调研报告》; 商机决策 needs《决策评审纪要》.
 */
export const OPP_TRANSITION_ARTIFACT: Partial<
  Record<OpportunityStage, { type: string; label: string }>
> = {
  LEAD: { type: 'RESEARCH_REPORT', label: '商机调研报告' },
  OPPORTUNITY: { type: 'DECISION_MINUTES', label: '决策评审纪要' },
};

export interface Opportunity {
  id: number;
  customerName: string;
  title: string;
  note?: string | null;
  amount?: number | null;
  stage: OpportunityStage;
  status: OpportunityStatus;
  commercialOwnerUserId?: number | null;
  commercialOwnerName?: string | null;
  solutionOwnerUserId?: number | null;
  solutionOwnerName?: string | null;
  pmUserId?: number | null;
  pmName?: string | null;
  opsOwnerUserId?: number | null;
  opsOwnerName?: string | null;
  projectId?: number | null;
  productId?: number | null;
  productName?: string | null;
  customerId?: number | null;
  gateDecidedBy?: string | null;
  /** v0.0.47 — 进入当前阶段的时刻（停留时长预警）。 */
  stageEnteredAt?: string | null;
  createTime?: string;
}

export interface OpportunityCreate {
  customerName: string;
  title: string;
  note?: string;
  amount?: number;
  commercialOwnerUserId?: number;
  solutionOwnerUserId?: number;
  pmUserId?: number;
  opsOwnerUserId?: number;
  productId?: number;
  customerId?: number;
}

/** Edit an opportunity's attributes (not stage/status — those go through advance/initiate). */
export interface OpportunityUpdate {
  customerName: string;
  title: string;
  note?: string;
  amount?: number;
  commercialOwnerUserId?: number;
  solutionOwnerUserId?: number;
  pmUserId?: number;
  opsOwnerUserId?: number;
  productId?: number;
  customerId?: number;
}

export interface OpportunityListParams {
  stage?: OpportunityStage;
  status?: OpportunityStatus;
  ownerUserId?: number;
  page?: number;
  size?: number;
}

export async function listOpportunities(
  params: OpportunityListParams = {},
): Promise<PaginatedResult<Opportunity>> {
  const res = await client.get<PaginatedResult<Opportunity>>('/opportunities', { params });
  return res.data;
}

export async function createOpportunity(body: OpportunityCreate): Promise<Opportunity> {
  const res = await client.post<Opportunity>('/opportunities', body);
  return res.data;
}

export async function updateOpportunity(
  id: number,
  body: OpportunityUpdate,
): Promise<Opportunity> {
  const res = await client.put<Opportunity>(`/opportunities/${id}`, body);
  return res.data;
}

/** Inline 流转产出物 carried by advance when the source stage requires one (v0.0.45). */
export interface ArtifactInput {
  title: string;
  content: string;
}

/**
 * Advance one stage. At a gate stage, pass decision PASS/REJECT (REJECT loses the deal). v0.0.45: when
 * the source stage requires a 产出物 (see OPP_TRANSITION_ARTIFACT), pass `artifact` — the backend creates
 * it + advances atomically (missing → 400).
 */
export async function advanceOpportunity(
  id: number,
  decision?: GateDecision,
  note?: string,
  artifact?: ArtifactInput,
): Promise<Opportunity> {
  const res = await client.post<Opportunity>(`/opportunities/${id}/advance`, {
    decision,
    note,
    artifact,
  });
  return res.data;
}

/**
 * 立项 (v0.0.48): link an existing 对外-交付 Project OR inline-create one (atomic, backend-side).
 * 二选一：传 `projectId`，或传 `projectCode`+`projectName`（可选 `projectOwnerUserId`，默认商机 pmUserId）。
 */
export interface OpportunityInitiate {
  decision: GateDecision;
  note?: string;
  projectId?: number;
  projectCode?: string;
  projectName?: string;
  projectOwnerUserId?: number;
}

export async function initiateOpportunity(
  id: number,
  body: OpportunityInitiate,
): Promise<Opportunity> {
  const res = await client.post<Opportunity>(`/opportunities/${id}/initiate`, body);
  return res.data;
}
