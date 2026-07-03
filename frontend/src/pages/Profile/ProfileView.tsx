import { DashboardCard, EmptyState, OwnerChip, StatTiles, StatusChip } from '../../components/board';
import type { UserProfile } from '../../api/profile';

const ORG_TYPE_LABEL: Record<string, string> = {
  COMPANY: '公司',
  DEPARTMENT: '部门',
  DOMAIN: '领域',
  TEAM: '团队',
  SUBGROUP: '小组',
};
const ROLE_LABEL: Record<string, string> = { HEAD: '负责人', MEMBER: '成员' };
const CAPABILITY_CATEGORY_LABEL: Record<string, string> = {
  TECH: '技术',
  PRODUCT: '产品',
  SOFT: '软技能',
};
const CAPABILITY_SOURCE_LABEL: Record<string, string> = { SELF: '自评', MANAGER: '主管' };

interface ProfileViewProps {
  title: string;
  profile: UserProfile;
  perspective?: 'self' | 'member';
}

/** Shared read-only profile renderer for both /profile and /users/:id/profile. */
export function ProfileView({ title, profile, perspective = 'self' }: ProfileViewProps) {
  const displayName = profile.name ?? profile.loginName;
  const capabilities = profile.capabilities ?? [];
  const isSelf = perspective === 'self';

  return (
    <div className="rainier-page">
      <div className="rainier-page-head">
        <h2>{title}</h2>
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
          { label: isSelf ? '我负责的 Story' : '负责的 Story', value: profile.ownedStoryCount },
          { label: isSelf ? '分配给我的任务' : '分配的任务', value: profile.assignedTaskCount },
        ]}
      />

      <DashboardCard title="能力标签" testId="profile-capabilities">
        {capabilities.length === 0 ? (
          <EmptyState message="暂未维护能力标签。" testId="profile-capabilities-empty" />
        ) : (
          <table className="rainier-list-table">
            <tbody>
              {capabilities.map((c) => (
                <tr key={c.capabilityTagId} data-testid={`profile-capability-${c.capabilityTagId}`}>
                  <td style={{ padding: '6px 8px', fontWeight: 600 }}>
                    {c.tagName ?? `#${c.capabilityTagId}`}
                  </td>
                  <td style={{ padding: '6px 8px', width: 120 }}>
                    <StatusChip
                      status={c.tagCategory ?? 'UNKNOWN'}
                      label={
                        c.tagCategory
                          ? (CAPABILITY_CATEGORY_LABEL[c.tagCategory] ?? c.tagCategory)
                          : '未分类'
                      }
                      tier={c.tagCategory === 'TECH' ? 'yellow' : c.tagCategory === 'SOFT' ? 'green' : 'gray'}
                    />
                  </td>
                  <td style={{ padding: '6px 8px', width: 80 }}>L{c.level}</td>
                  <td style={{ padding: '6px 8px', width: 100, color: 'var(--rainier-color-text-2)' }}>
                    {c.source ? (CAPABILITY_SOURCE_LABEL[c.source] ?? c.source) : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </DashboardCard>

      <DashboardCard title="组织身份" testId="profile-orgs">
        {profile.memberships.length === 0 ? (
          <EmptyState message={isSelf ? '你还没有组织归属。' : '暂无组织归属。'} testId="profile-orgs-empty" />
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
