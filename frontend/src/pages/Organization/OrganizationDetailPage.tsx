import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { StatusChip } from '../../components/board';
import { Card } from '../../components/ui/Card';
import { Table, type TableColumn } from '../../components/ui/Table';
import {
  getOrganization,
  listOrganizations,
  listOrganizationAuditLog,
  type Organization,
} from '../../api/organization';
import {
  listOrganizationPmos,
  type OrganizationPmoDetail,
} from '../../api/organizationPmo';
import { listUserOrganizations, type UserOrganization } from '../../api/userOrganization';
import { listProjects, type Project } from '../../api/project';
import type { AuditLog } from '../../api/auditLog';
import { formatDateTime } from '../../utils/formatDate';
import { isElevated, useAuthStore } from '../../store/auth';

type Tab = 'info' | 'members' | 'pmos' | 'children' | 'projects' | 'audit';

/**
 * v0.0.99 (E3) — Organization 详情独立页 (/org/orgs/:id, admin only).
 *
 * <p>Tabs: 基本信息 / 成员 / PMO / 子组织 / 关联项目 / 变更历史.
 * 编辑仍走 OrganizationsPage 的 EditDrawer (本版只做只读视图 + 历史)。
 */
export function OrganizationDetailPage() {
  const { id: idParam } = useParams<{ id: string }>();
  const id = Number(idParam);
  const user = useAuthStore((s) => s.user);
  const elevated = isElevated(user);

  const [org, setOrg] = useState<Organization | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tab, setTab] = useState<Tab>('info');

  const [members, setMembers] = useState<UserOrganization[]>([]);
  const [pmos, setPmos] = useState<OrganizationPmoDetail[]>([]);
  const [children, setChildren] = useState<Organization[]>([]);
  const [projects, setProjects] = useState<Project[]>([]);
  const [audit, setAudit] = useState<AuditLog[]>([]);

  const load = useCallback(async () => {
    if (!Number.isFinite(id)) {
      setError('无效的组织 ID');
      setLoading(false);
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const o = await getOrganization(id);
      setOrg(o);
    } catch (e) {
      setError((e as Error).message ?? '加载失败');
    } finally {
      setLoading(false);
    }
  }, [id]);

  useEffect(() => {
    void load();
  }, [load]);

  // Lazy tab loaders.
  useEffect(() => {
    if (!org) return;
    let cancel = false;
    if (tab === 'members') {
      void listUserOrganizations({ organizationId: id, activeOnly: true, size: 200 }).then((r) => {
        if (!cancel) setMembers(r.content ?? []);
      });
    } else if (tab === 'pmos') {
      void listOrganizationPmos(id).then((r) => {
        if (!cancel) setPmos(r);
      });
    } else if (tab === 'children') {
      void listOrganizations({ parentId: id, size: 200 }).then((r) => {
        if (!cancel) setChildren(r.content ?? []);
      });
    } else if (tab === 'projects') {
      void listProjects({ size: 200 }).then((r) => {
        if (!cancel) setProjects((r.content ?? []).filter((p) => p.organizationId === id));
      });
    } else if (tab === 'audit') {
      void listOrganizationAuditLog(id, { size: 50 }).then((r) => {
        if (!cancel) setAudit(r.content ?? []);
      });
    }
    return () => {
      cancel = true;
    };
  }, [tab, id, org]);

  if (!elevated) {
    return (
      <div className="rainier-page" data-testid="org-detail-forbidden">
        <Card>仅管理员可以查看组织详情。</Card>
      </div>
    );
  }
  if (loading) return <div className="rainier-page">加载中…</div>;
  if (error || !org) {
    return (
      <div className="rainier-page" data-testid="org-detail-error">
        <Card>{error ?? '组织不存在'}</Card>
      </div>
    );
  }

  const memberCols: TableColumn<UserOrganization>[] = [
    { key: 'name', title: '姓名', render: (r) => r.userName ?? r.userLoginName ?? `#${r.userId}` },
    { key: 'role', title: '角色', render: (r) => <StatusChip status={r.role} /> },
    { key: 'isPrimary', title: '主组织', render: (r) => (r.isPrimary ? '是' : '') },
    { key: 'joinedAt', title: '加入时间', render: (r) => formatDateTime(r.joinedAt) },
  ];
  const pmoCols: TableColumn<OrganizationPmoDetail>[] = [
    { key: 'name', title: 'PMO', render: (r) => r.userName ?? r.userLoginName ?? `#${r.userId}` },
    { key: 'createTime', title: '指派时间', render: (r) => formatDateTime(r.createTime) },
  ];
  const childCols: TableColumn<Organization>[] = [
    {
      key: 'name',
      title: '名称',
      render: (r) => <Link to={`/org/orgs/${r.id}`}>{r.name}</Link>,
    },
    { key: 'code', title: '编码', render: (r) => r.code },
    { key: 'type', title: '类型', render: (r) => <StatusChip status={r.type} /> },
    { key: 'enabled', title: '启用', render: (r) => (r.enabled ? '是' : '否') },
  ];
  const projectCols: TableColumn<Project>[] = [
    {
      key: 'name',
      title: '名称',
      render: (r) => <Link to={`/pm/projects/${r.id}`}>{r.name}</Link>,
    },
    { key: 'code', title: '编码', render: (r) => r.code },
    { key: 'status', title: '状态', render: (r) => <StatusChip status={r.status} /> },
  ];
  const auditCols: TableColumn<AuditLog>[] = [
    { key: 'time', title: '时间', render: (r) => formatDateTime(r.createTime) },
    { key: 'actor', title: '操作人', render: (r) => r.actor ?? 'system' },
    { key: 'action', title: '动作', render: (r) => <StatusChip status={r.action} /> },
    { key: 'summary', title: '摘要', render: (r) => r.summary ?? '' },
  ];

  return (
    <div className="rainier-page" data-testid="org-detail">
      <div className="rainier-page-head">
        <h2>{org.name}</h2>
        <StatusChip status={org.type} />
        <span style={{ color: 'var(--rainier-color-text-3)' }}>{org.code}</span>
        {!org.enabled && <StatusChip status="DISABLED" label="已停用" />}
        <span className="rainier-spacer" />
      </div>

      <div className="pm-tabs" data-testid="org-detail-tabs">
        <button type="button" className="pm-tab" data-active={tab === 'info'} onClick={() => setTab('info')}>
          基本信息
        </button>
        <button type="button" className="pm-tab" data-active={tab === 'members'} onClick={() => setTab('members')}>
          成员
        </button>
        <button type="button" className="pm-tab" data-active={tab === 'pmos'} onClick={() => setTab('pmos')}>
          PMO
        </button>
        <button type="button" className="pm-tab" data-active={tab === 'children'} onClick={() => setTab('children')}>
          子组织
        </button>
        <button type="button" className="pm-tab" data-active={tab === 'projects'} onClick={() => setTab('projects')}>
          关联项目
        </button>
        <button
          type="button"
          className="pm-tab"
          data-active={tab === 'audit'}
          onClick={() => setTab('audit')}
          data-testid="org-tab-audit"
        >
          变更历史
        </button>
      </div>

      {tab === 'info' && (
        <Card>
          <div className="pm-info-grid">
            <span className="pm-info-label">名称</span>
            <span className="pm-info-value">{org.name}</span>
            <span className="pm-info-label">编码</span>
            <span className="pm-info-value">{org.code}</span>
            <span className="pm-info-label">类型</span>
            <span className="pm-info-value">{org.type}</span>
            <span className="pm-info-label">全路径</span>
            <span className="pm-info-value">{org.wholeName ?? '—'}</span>
            <span className="pm-info-label">启用</span>
            <span className="pm-info-value">{org.enabled ? '是' : '否'}</span>
            <span className="pm-info-label">描述</span>
            <span className="pm-info-value">{org.description ?? '—'}</span>
            <span className="pm-info-label">创建/更新</span>
            <span className="pm-info-value">
              {formatDateTime(org.createTime)} · {formatDateTime(org.updateTime)}
            </span>
          </div>
        </Card>
      )}
      {tab === 'members' && (
        <Card>
          <Table<UserOrganization> columns={memberCols} dataSource={members} rowKey="id" />
        </Card>
      )}
      {tab === 'pmos' && (
        <Card>
          <Table<OrganizationPmoDetail> columns={pmoCols} dataSource={pmos} rowKey="id" />
        </Card>
      )}
      {tab === 'children' && (
        <Card>
          <Table<Organization> columns={childCols} dataSource={children} rowKey="id" />
        </Card>
      )}
      {tab === 'projects' && (
        <Card>
          <Table<Project> columns={projectCols} dataSource={projects} rowKey="id" />
        </Card>
      )}
      {tab === 'audit' && (
        <Card>
          <Table<AuditLog> columns={auditCols} dataSource={audit} rowKey="id" />
        </Card>
      )}
    </div>
  );
}
