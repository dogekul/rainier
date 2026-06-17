import { useCallback, useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { DashboardCard, EmptyState, StatusBar, StatusChip } from '../../components/board';
import { isOverdue, todayISO } from '../../utils/board';
import { isOpenTaskStatus, loadTier, ryg, RYG_ORDER, type RygTier } from '../../utils/ryg';
import { useAuthStore } from '../../store/auth';
import { listLedTeams, listTeamMembers, type LedTeam } from '../../api/teamLead';
import { listTasks } from '../../api/task';
import { listStories } from '../../api/story';

const PAGE = 200;

interface MemberLoad {
  userId: number;
  name: string;
  openTasks: number;
  openStories: number;
}

interface ProjectHealth {
  projectId: number;
  label: string;
  tier: RygTier;
  openCount: number;
  overdueCount: number;
}

/**
 * v0.0.25 — 团队负责人面板 (Team-Lead Panel). For an org HEAD: per-member open-work load + a
 * red/yellow/green ranking of the lead's projects, by pure rules (no AI). Consumes the v0.0.24
 * self-scoped /api/me/* endpoints + reused list endpoints. All-users (a lead is not an admin).
 */
export function TeamLeadPage() {
  const user = useAuthStore((s) => s.user);
  const projects = useMemo(() => user?.projects ?? [], [user]);
  const today = todayISO();

  const [teams, setTeams] = useState<LedTeam[] | null>(null);
  const [teamId, setTeamId] = useState<number | null>(null);
  const [loads, setLoads] = useState<MemberLoad[]>([]);
  const [health, setHealth] = useState<ProjectHealth[]>([]);

  // Which teams do I lead? Auto-select the sole team (0 clicks) or default to the first.
  useEffect(() => {
    let cancelled = false;
    void listLedTeams()
      .then((t) => {
        if (cancelled) return;
        setTeams(t);
        if (t.length > 0) setTeamId(t[0].organizationId);
      })
      .catch(() => {
        if (!cancelled) setTeams([]);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // Member load for the selected team.
  useEffect(() => {
    if (teamId == null) return;
    let active = true;
    setLoads([]);
    void (async () => {
      const members = await listTeamMembers(teamId);
      const rows = await Promise.all(
        members.map(async (m) => {
          const [t, s] = await Promise.all([
            listTasks({ assigneeUserId: m.userId, size: PAGE }),
            listStories({ ownerUserId: m.userId, size: PAGE }),
          ]);
          return {
            userId: m.userId,
            name: m.name || m.loginName || `#${m.userId}`,
            openTasks: t.content.filter((x) => isOpenTaskStatus(x.status)).length,
            openStories: s.content.filter((x) => isOpenTaskStatus(x.status)).length,
          };
        }),
      );
      if (active) setLoads(rows);
    })().catch(() => {
      if (active) setLoads([]);
    });
    return () => {
      active = false;
    };
  }, [teamId]);

  // Project RYG ranking over the lead's projects (independent of the selected team).
  const loadHealth = useCallback(async () => {
    const rows = await Promise.all(
      projects.map(async (p) => {
        const tasks = (await listTasks({ projectId: p.id, size: PAGE })).content;
        const open = tasks.filter((t) => isOpenTaskStatus(t.status));
        const overdue = open.filter((t) => isOverdue(t.dueDate, today));
        const anyBlocked = open.some((t) => t.status === 'BLOCKED');
        return {
          projectId: p.id,
          label: `${p.code} ${p.name}`,
          tier: ryg({ openCount: open.length, overdueCount: overdue.length, anyBlocked }),
          openCount: open.length,
          overdueCount: overdue.length,
        };
      }),
    );
    rows.sort((a, b) => RYG_ORDER[a.tier] - RYG_ORDER[b.tier]);
    setHealth(rows);
  }, [projects, today]);

  useEffect(() => {
    if (projects.length === 0) {
      setHealth([]);
      return;
    }
    void loadHealth().catch(() => setHealth([]));
  }, [projects, loadHealth]);

  if (teams != null && teams.length === 0) {
    return (
      <EmptyState message="你当前不是任何团队的负责人。" testId="team-lead-empty" />
    );
  }

  const maxLoad = Math.max(1, ...loads.map((l) => l.openTasks));
  const tierLabel: Record<RygTier, string> = { red: '红', yellow: '黄', green: '绿', gray: '灰' };
  // Map a RYG tier to a representative status so StatusChip picks the right colour.
  const tierStatus: Record<RygTier, string> = {
    red: 'BLOCKED',
    yellow: 'IN_PROGRESS',
    green: 'DONE',
    gray: 'DRAFT',
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <h2 style={{ margin: 0 }}>团队负责人面板</h2>
        {teams && teams.length > 1 && (
          <select
            data-testid="tl-team-select"
            className="rainier-treeselect-trigger"
            value={teamId ?? ''}
            onChange={(e) => setTeamId(Number(e.target.value))}
          >
            {teams.map((t) => (
              <option key={t.organizationId} value={t.organizationId}>
                {t.organizationName}
              </option>
            ))}
          </select>
        )}
        {teams && teams.length === 1 && (
          <span style={{ color: 'var(--rainier-color-text-2)' }}>{teams[0].organizationName}</span>
        )}
      </div>

      <DashboardCard title="成员负载" extra={`${loads.length} 人`} testId="tl-members">
        {loads.length === 0 ? (
          <p style={{ color: 'var(--rainier-color-text-2)', margin: 0 }}>暂无成员或正在加载…</p>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <tbody>
              {loads.map((m) => (
                <tr key={m.userId} data-testid={`tl-member-${m.userId}`}>
                  <td style={{ padding: '6px 8px', width: 120 }}>
                    <Link to={`/pm/tasks?assigneeUserId=${m.userId}`}>{m.name}</Link>
                  </td>
                  <td style={{ padding: '6px 8px' }}>
                    <StatusBar
                      testId={`tl-member-load-${m.userId}`}
                      showLegend={false}
                      max={maxLoad}
                      segments={[
                        { label: '开放任务', count: m.openTasks, tier: loadTier(m.openTasks) },
                      ]}
                    />
                  </td>
                  <td
                    style={{ padding: '6px 8px', width: 160, color: 'var(--rainier-color-text-2)' }}
                  >
                    开放任务 {m.openTasks} · Story {m.openStories}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </DashboardCard>

      <DashboardCard title="项目红黄绿" extra={`${health.length} 个项目`} testId="tl-projects">
        {health.length === 0 ? (
          <p style={{ color: 'var(--rainier-color-text-2)', margin: 0 }}>暂无项目。</p>
        ) : (
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <tbody>
              {health.map((h) => (
                <tr key={h.projectId} data-testid={`tl-project-${h.projectId}`}>
                  <td style={{ padding: '6px 8px', width: 60 }}>
                    <StatusChip
                      status={tierStatus[h.tier]}
                      label={tierLabel[h.tier]}
                      testId={`tl-project-tier-${h.projectId}`}
                    />
                  </td>
                  <td style={{ padding: '6px 8px' }}>
                    <Link to={`/pm/tasks?projectId=${h.projectId}`}>{h.label}</Link>
                  </td>
                  <td
                    style={{ padding: '6px 8px', width: 180, color: 'var(--rainier-color-text-2)' }}
                  >
                    开放 {h.openCount} · 逾期 {h.overdueCount}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </DashboardCard>
    </div>
  );
}
