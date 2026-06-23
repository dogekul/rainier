import client from './client';

export type ArtifactType =
  | 'RESEARCH_REPORT'
  | 'DECISION_MINUTES'
  | 'PRESENTATION_MATERIAL'
  | 'CLIENT_REQUIREMENTS'
  | 'POC_SCORE'
  | 'GAP_ANALYSIS';

/** Mirrors backend ArtifactType.LABELS. */
export const ARTIFACT_TYPE_LABELS: Record<ArtifactType, string> = {
  RESEARCH_REPORT: '商机调研报告',
  DECISION_MINUTES: '决策评审纪要',
  PRESENTATION_MATERIAL: '讲解材料',
  CLIENT_REQUIREMENTS: '甲方诉求清单',
  POC_SCORE: 'POC 得分表',
  GAP_ANALYSIS: '差距分析报告',
};

/** LINK-kind types carry a URL; others carry rich-text content (mirrors backend ArtifactType.LINK_TYPES). */
export const ARTIFACT_LINK_TYPES: ArtifactType[] = ['PRESENTATION_MATERIAL', 'CLIENT_REQUIREMENTS'];
export function isLinkArtifact(type: string): boolean {
  return (ARTIFACT_LINK_TYPES as string[]).includes(type);
}

/**
 * Mirrors backend TransitionArtifactRules for MULTI-artifact stages — the types required to advance FROM
 * a stage. Single-artifact inline stages (线索/商机) live in OPP_TRANSITION_ARTIFACT instead; this drives
 * the「推进时补充」multi-doc form (推介/POC → 投标).
 */
export const STAGE_REQUIRED_ARTIFACTS: Record<string, ArtifactType[]> = {
  POC: ['PRESENTATION_MATERIAL', 'CLIENT_REQUIREMENTS', 'POC_SCORE', 'GAP_ANALYSIS'],
};

/** Types offerable in the「添加产出物」picker. */
export const ADDABLE_ARTIFACT_TYPES: ArtifactType[] = [
  'PRESENTATION_MATERIAL',
  'CLIENT_REQUIREMENTS',
  'POC_SCORE',
  'GAP_ANALYSIS',
  'RESEARCH_REPORT',
  'DECISION_MINUTES',
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
