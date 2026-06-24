import client from './client';

export type ArtifactType =
  | 'RESEARCH_REPORT'
  | 'DECISION_MINUTES'
  | 'PRESENTATION_MATERIAL'
  | 'CLIENT_REQUIREMENTS'
  | 'POC_SCORE'
  | 'GAP_ANALYSIS'
  // v0.0.46 投标/合同 types (附件先 URL 占位 → 链接类，评审会议纪要为报告类):
  | 'BID_DOCUMENT'
  | 'BID_WINNING_NOTICE'
  | 'CONTRACT_DRAFT'
  | 'CONTRACT_REVIEW_MINUTES'
  | 'REVIEW_EMAIL_ARCHIVE'
  | 'SIGNED_CONTRACT'
  // v0.0.53 现场调研(SURVEY)-gate types：报告类正文 + 附件类 URL。
  | 'SURVEY_REPORT'
  | 'SURVEY_ATTACHMENT'
  // v0.0.57 交付实施(DELIVERY)-gate type：甲方验收报告（报告类）。
  | 'DELIVERY_ACCEPTANCE_REPORT';

/** Mirrors backend ArtifactType.LABELS. */
export const ARTIFACT_TYPE_LABELS: Record<ArtifactType, string> = {
  RESEARCH_REPORT: '商机调研报告',
  DECISION_MINUTES: '决策评审纪要',
  PRESENTATION_MATERIAL: '讲解材料',
  CLIENT_REQUIREMENTS: '甲方诉求清单',
  POC_SCORE: 'POC 得分表',
  GAP_ANALYSIS: '差距分析报告',
  BID_DOCUMENT: '投标文件',
  BID_WINNING_NOTICE: '中标公示',
  CONTRACT_DRAFT: '合同',
  CONTRACT_REVIEW_MINUTES: '评审会议纪要',
  REVIEW_EMAIL_ARCHIVE: '邮件归档',
  SIGNED_CONTRACT: '已盖章合同',
  SURVEY_REPORT: '现场调研报告',
  SURVEY_ATTACHMENT: '现场调研附件',
  DELIVERY_ACCEPTANCE_REPORT: '甲方验收报告',
};

/** LINK-kind types carry a URL; others carry rich-text content (mirrors backend ArtifactType.LINK_TYPES). */
export const ARTIFACT_LINK_TYPES: ArtifactType[] = [
  'PRESENTATION_MATERIAL',
  'CLIENT_REQUIREMENTS',
  'BID_DOCUMENT',
  'BID_WINNING_NOTICE',
  'CONTRACT_DRAFT',
  'REVIEW_EMAIL_ARCHIVE',
  'SIGNED_CONTRACT',
  'SURVEY_ATTACHMENT',
];
export function isLinkArtifact(type: string): boolean {
  return (ARTIFACT_LINK_TYPES as string[]).includes(type);
}

/**
 * Mirrors backend TransitionArtifactRules for MULTI-artifact stages — the types required to advance FROM
 * a stage. Single-artifact inline stages (线索/商机) live in OPP_TRANSITION_ARTIFACT instead; this drives
 * the「推进时补充」multi-doc form (推介/POC → 投标; 投标 → 合同; 合同 → 立项). Gate stages here gate PASS only —
 * 否决(REJECT) skips the form (see PresaleFlow.requestAdvance), mirroring backend PASS-only enforcement.
 */
export const STAGE_REQUIRED_ARTIFACTS: Record<string, ArtifactType[]> = {
  POC: ['PRESENTATION_MATERIAL', 'CLIENT_REQUIREMENTS', 'POC_SCORE', 'GAP_ANALYSIS'],
  BIDDING: ['BID_DOCUMENT'],
  CONTRACT: [
    'BID_WINNING_NOTICE',
    'CONTRACT_DRAFT',
    'CONTRACT_REVIEW_MINUTES',
    'REVIEW_EMAIL_ARCHIVE',
    'SIGNED_CONTRACT',
  ],
  // v0.0.53 现场调研 → 产品诉求：报告 + 附件 全齐才能推进。
  SURVEY: ['SURVEY_REPORT', 'SURVEY_ATTACHMENT'],
  // v0.0.57 交付实施 → 验收：甲方验收报告（报告类，单件）。
  DELIVERY: ['DELIVERY_ACCEPTANCE_REPORT'],
};

/** Types offerable in the「添加产出物」picker. */
export const ADDABLE_ARTIFACT_TYPES: ArtifactType[] = [
  'PRESENTATION_MATERIAL',
  'CLIENT_REQUIREMENTS',
  'POC_SCORE',
  'GAP_ANALYSIS',
  'RESEARCH_REPORT',
  'DECISION_MINUTES',
  'BID_DOCUMENT',
  'BID_WINNING_NOTICE',
  'CONTRACT_DRAFT',
  'CONTRACT_REVIEW_MINUTES',
  'REVIEW_EMAIL_ARCHIVE',
  'SIGNED_CONTRACT',
  'SURVEY_REPORT',
  'SURVEY_ATTACHMENT',
  'DELIVERY_ACCEPTANCE_REPORT',
];

export interface OpportunityArtifact {
  id: number;
  opportunityId: number;
  type: ArtifactType;
  typeLabel: string;
  stageFrom?: string | null;
  title: string;
  content?: string | null;
  link?: string | null;
  decision?: string | null;
  author?: string | null;
  createTime?: string;
}

export interface OpportunityArtifactCreate {
  type: ArtifactType;
  /** Optional — 链接类材料无需标题；为空时后端用类型名兜底。 */
  title?: string;
  content?: string;
  link?: string;
}

/** v0.0.45 — list a 商机's 流转产出物 (append-only, newest first). */
export async function listOpportunityArtifacts(
  opportunityId: number,
): Promise<OpportunityArtifact[]> {
  const res = await client.get<OpportunityArtifact[]>(
    `/opportunities/${opportunityId}/artifacts`,
  );
  return res.data;
}

/** v0.0.45 — independently add a 流转产出物 (prepare POC deliverables before advancing). */
export async function createOpportunityArtifact(
  opportunityId: number,
  body: OpportunityArtifactCreate,
): Promise<OpportunityArtifact> {
  const res = await client.post<OpportunityArtifact>(
    `/opportunities/${opportunityId}/artifacts`,
    body,
  );
  return res.data;
}

/** v0.0.45 — fetch one artifact's .docx (with bearer) and trigger a browser download. */
export async function exportArtifactDocx(
  opportunityId: number,
  artifactId: number,
  filename: string,
): Promise<void> {
  const res = await client.get<Blob>(
    `/opportunities/${opportunityId}/artifacts/${artifactId}/export`,
    { responseType: 'blob' },
  );
  const url = URL.createObjectURL(res.data);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  URL.revokeObjectURL(url);
}
