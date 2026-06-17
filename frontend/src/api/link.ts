import client from './client';

export type LinkTargetType = 'STORY' | 'TASK';
export type LinkType = 'PRD' | 'DESIGN' | 'DEFECT' | 'TESTCASE' | 'PR' | 'OTHER';

/** v0.0.31 — 中文 labels for the link types (panel dropdown + chip). */
export const LINK_TYPE_LABELS: Record<LinkType, string> = {
  PRD: 'PRD',
  DESIGN: '设计稿',
  DEFECT: '缺陷',
  TESTCASE: '用例',
  PR: '代码PR',
  OTHER: '其他',
};

export interface EntityLink {
  id: number;
  targetType: LinkTargetType;
  targetId: number;
  linkType: LinkType;
  label?: string | null;
  url: string;
  createBy?: string;
  createTime?: string;
}

export interface LinkCreate {
  targetType: LinkTargetType;
  targetId: number;
  linkType: LinkType;
  label?: string;
  url: string;
}

/** GET /api/links?targetType=&targetId= — a Story/Task's external-artifact links (oldest-first). */
export async function listLinks(
  targetType: LinkTargetType,
  targetId: number,
): Promise<EntityLink[]> {
  const res = await client.get<EntityLink[]>('/links', { params: { targetType, targetId } });
  return res.data;
}

export async function createLink(body: LinkCreate): Promise<EntityLink> {
  const res = await client.post<EntityLink>('/links', body);
  return res.data;
}

export async function deleteLink(id: number): Promise<void> {
  await client.delete(`/links/${id}`);
}
