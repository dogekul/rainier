import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { DashboardCard, EmptyState, StatusBar, StatusChip } from '../../components/board';
import { rygToTier, RYG_LABEL } from '../../utils/board';
import { isOpenTaskStatus, loadTier } from '../../utils/ryg';
import { listLedTeams, listTeamMembers, type LedTeam } from '../../api/teamLead';
import { getPortfolio, type PortfolioRow } from '../../api/portfolio';
import { listTasks } from '../../api/task';
import { listStories } from '../../api/story';

const PAGE = 200;

interface MemberLoad {
  userId: number;
  name: string;
  openTasks: number;
  openStories: number;
}

/**
 * v0.0.25 — 团队负责人面板 (Team-Lead Panel). For an org HEAD: per-member open-work load + a
 * red/yellow/green ranking of the team's projects, by pure rules (no AI).
 *
 * <p>v0.0.29: the project RYG now uses GET /api/me/portfolio?scope=led — the projects under the orgs
 * the lead HEADs (and their org-subtree), NOT the lead's personal projects. This fixes the mis-scope.
 */
export function TeamLeadPage() {
  const [teams, setTeams] = useState<LedTeam[] | null>(null);
  const [teamId, setTeamId] = useState<number | null>(null);
  const [loads, setLoads] = useState<MemberLoad[]>([]);
  const [health, setHealth] = useState<PortfolioRow[]>([]);

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

  // Project RYG over the TEAM's footprint (orgs I lead + subtree), server-computed + worst-first sorted.
  useEffect(() => {
    let active = true;
    void getPortfolio('led')
      .then((rows) => {
        if (active) setHealth(rows);
      })
      .catch(() => {
        if (active) setHealth([]);
      });
    return () => {
      active = false;
    };
  }, []);

  if (teams != null && teams.length === 0) {
    return (
      <EmptyState message="你当前不是任何团队的负责人。" testId="team-lead-empty" />
    );
  }

  const maxLoad = Math.max(1, ...loads.map((l) => l.openTasks));

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>团队负责人面板</h2>
        {teams && teams.length > 1 && (
          <select
            data-testid="tl-team-select"
            className="rainier-select"
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
          <table className="rainier-list-table">
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
          <table className="rainier-list-table">
            <tbody>
              {health.map((h) => (
                <tr key={h.projectId} data-testid={`tl-project-${h.projectId}`}>
                  <td style={{ padding: '6px 8px', width: 60 }}>
                    <StatusChip
                      status=""
                      tier={rygToTier(h.ryg)}
                      label={RYG_LABEL[rygToTier(h.ryg)]}
                      testId={`tl-project-tier-${h.projectId}`}
                    />
                  </td>
                  <td style={{ padding: '6px 8px' }}>
                    <Link to={`/pm/tasks?projectId=${h.projectId}`}>
                      {h.projectCode} {h.projectName}
                    </Link>
                  </td>
                  <td
                    style={{ padding: '6px 8px', width: 180, color: 'var(--rainier-color-text-2)' }}
                  >
                    开放 {h.openTasks} · 逾期 {h.overdueTasks}
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
