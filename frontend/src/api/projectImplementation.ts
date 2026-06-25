import client from './client';

/**
 * v0.0.89 — Project 立项后施工内容表单。1:1 关联 projectId（upsert）。
 */
export interface ProjectImplementation {
  id: number;
  projectId: number;
  scopeMarkdown: string;
  estimatedManDays?: number | null;
  riskNotes?: string | null;
  keyMilestonesJson?: string | null;
  createTime?: string;
  updateTime?: string;
}

export interface ProjectImplementationUpsert {
  scopeMarkdown: string;
  estimatedManDays?: number | null;
  riskNotes?: string | null;
  keyMilestonesJson?: string | null;
}

/** Returns the implementation form for a project, or `null` if it doesn't exist yet (404). */
export async function getProjectImplementation(
  projectId: number,
): Promise<ProjectImplementation | null> {
  try {
    const res = await client.get<ProjectImplementation>(`/projects/${projectId}/implementation`);
    return res.data;
  } catch (e) {
    const err = e as { response?: { status?: number } };
    if (err?.response?.status === 404) return null;
    throw e;
  }
}

export async function upsertProjectImplementation(
  projectId: number,
  body: ProjectImplementationUpsert,
): Promise<ProjectImplementation> {
  const res = await client.put<ProjectImplementation>(
    `/projects/${projectId}/implementation`,
    body,
  );
  return res.data;
}
