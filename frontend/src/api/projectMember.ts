import client from './client';

/** v0.0.64 — Project member API + role enum + Chinese labels. */

export type ProjectMemberRole =
  | 'PD'
  | 'DEV'
  | 'QA'
  | 'DESIGN'
  | 'BIZ'
  | 'OPS'
  | 'OTHER';

/** Real assignable roles (used by 添加成员 dropdown). */
export const PROJECT_MEMBER_ROLE_OPTIONS: ProjectMemberRole[] = [
  'PD',
  'DEV',
  'QA',
  'DESIGN',
  'BIZ',
  'OPS',
  'OTHER',
];

export const PROJECT_MEMBER_ROLE_LABELS: Record<ProjectMemberRole, string> = {
  PD: '产品经理',
  DEV: '研发',
  QA: '测试',
  DESIGN: '设计',
  BIZ: '业务',
  OPS: '运维',
  OTHER: '其他',
};

/**
 * 真实成员行 role 为 PROJECT_MEMBER_ROLE_OPTIONS 之一。
 * 合成行 role 为 `OWNER` / `PMO`（id 为 null）。
 */
export interface ProjectMemberDetail {
  id: number | null;
  projectId: number;
  userId: number;
  userName?: string | null;
  userLoginName?: string | null;
  role: string;
  displayLabel: string;
  joinedAt?: string | null;
  joinedBy?: string | null;
}

export async function listProjectMembers(projectId: number): Promise<ProjectMemberDetail[]> {
  const res = await client.get<ProjectMemberDetail[]>(`/projects/${projectId}/members`);
  return res.data;
}

export async function addProjectMember(
  projectId: number,
  userId: number,
  role: ProjectMemberRole,
): Promise<ProjectMemberDetail> {
  const res = await client.post<ProjectMemberDetail>(`/projects/${projectId}/members`, {
    userId,
    role,
  });
  return res.data;
}

export async function updateProjectMemberRole(
  projectId: number,
  userId: number,
  role: ProjectMemberRole,
): Promise<ProjectMemberDetail> {
  const res = await client.put<ProjectMemberDetail>(
    `/projects/${projectId}/members/${userId}`,
    { role },
  );
  return res.data;
}

export async function removeProjectMember(projectId: number, userId: number): Promise<void> {
  await client.delete(`/projects/${projectId}/members/${userId}`);
}
