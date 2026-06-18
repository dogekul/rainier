import { useEffect, useState } from 'react';
import { DashboardCard, EmptyState, OwnerChip, StatTiles, StatusChip } from '../../components/board';
import { getMyProfile, type UserProfile } from '../../api/profile';

const ORG_TYPE_LABEL: Record<string, string> = {
  COMPANY: '公司',
  DEPARTMENT: '部门',
  DOMAIN: '领域',
  TEAM: '团队',
  SUBGROUP: '小组',
};
const ROLE_LABEL: Record<string, string> = { HEAD: '负责人', MEMBER: '成员' };

/**
 * v0.0.40 —「我的档案」(my profile). Consumes GET /api/me/profile: the current user's org identity +
 * 岗位 + 直接上级 + contribution counts. All-users, read-only — the team-member growth landing the
 * audit (#9) found missing, and the base for team/subgroup/domain leads' people views.
 */
export function ProfilePage() {
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let active = true;
    void getMyProfile()
      .then((p) => {
        if (active) {
          setProfile(p);
          setLoading(false);
        }
      })
      .catch(() => {
        if (active) {
          setProfile(null);
          setLoading(false);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  if (loading || !profile) {
    return (
      <div className="rainier-page">
        <div className="rainier-page-head">
          <h2 style={{ margin: 0 }}>我的档案</h2>
        </div>
      </div>
    );
  }

  const displayName = profile.name ?? profile.loginName;

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2 style={{ margin: 0 }}>我的档案</h2>
      </div>

      <DashboardCard title="身份" testId="profile-identity">
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '4px 2px' }}>
          <OwnerChip name={displayName} loginName={profile.loginName} />
          <div style={{ color: 'var(--rainier-color-text-2)', fontSize: 13 }}>
            {profile.positionName ? (
              <span data-testid="profile-position">
                {profile.positionName}
                {profile.positionCategory ? ` · ${profile.positionCategory}` : ''}
              </span>
            ) : (
              <span>未定级</span>
            )}
            <span style={{ marginLeft: 10 }}>@{profile.loginName}</span>
          </div>
        </div>
        <div style={{ marginTop: 8, fontSize: 13 }}>
          <span style={{ color: 'var(--rainier-color-text-2)' }}>直接上级：</span>
          {profile.manager ? (
            <span data-testid="profile-manager">
              <OwnerChip name={profile.manager.name} loginName={profile.manager.loginName} />
            </span>
          ) : (
            <span data-testid="profile-manager-none">—</span>
          )}
        </div>
      </DashboardCard>

      <StatTiles
        testId="profile-stats"
        tiles={[
          { label: '我负责的 Story', value: profile.ownedStoryCount },
          { label: '分配给我的任务', value: profile.assignedTaskCount },
        ]}
      />

      <DashboardCard title="组织身份" testId="profile-orgs">
        {profile.memberships.length === 0 ? (
          <EmptyState message="你还没有组织归属。" testId="profile-orgs-empty" />
        ) : (
          <table className="rainier-list-table">
            <tbody>
              {profile.memberships.map((m) => (
                <tr key={m.organizationId} data-testid={`profile-org-${m.organizationId}`}>
                  <td style={{ padding: '6px 8px' }}>
                    {m.organizationName}
                    <span style={{ marginLeft: 8, color: 'var(--rainier-color-text-2)', fontSize: 12 }}>
                      {m.organizationType ? (ORG_TYPE_LABEL[m.organizationType] ?? m.organizationType) : ''}
                    </span>
                  </td>
                  <td style={{ padding: '6px 8px', width: 120 }}>
                    <StatusChip
                      status={m.role}
                      label={ROLE_LABEL[m.role] ?? m.role}
                      tier={m.role === 'HEAD' ? 'green' : 'gray'}
                      testId={`profile-role-${m.organizationId}`}
                    />
                  </td>
                  <td style={{ padding: '6px 8px', width: 80, color: 'var(--rainier-color-text-2)' }}>
                    {m.isPrimary ? '主组织' : ''}
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
