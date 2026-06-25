import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from '../../components/ui/Button';
import {
  getOpportunityFullLink,
  type FullLinkResponse,
  type FullLinkStageSummary,
} from '../../api/opportunity';

interface Props {
  opportunityId: number;
}

/**
 * v0.0.94 D6 — 商机↔项目↔运营 全链时间线面板。
 *
 * 项目栈未引入 Mantine —— 用纯 CSS 圆点+连线达到等价 Timeline UX。
 */
export function FullLinkPanel({ opportunityId }: Props) {
  const navigate = useNavigate();
  const [open, setOpen] = useState(false);
  const [data, setData] = useState<FullLinkResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    setLoading(true);
    setError(null);
    getOpportunityFullLink(opportunityId)
      .then((d) => setData(d))
      .catch((e: Error) => setError(e.message))
      .finally(() => setLoading(false));
  }, [opportunityId, open]);

  return (
    <div className="opp-card" data-testid="opp-fulllink-panel">
      <div className="opp-card-title">
        全链视图
        <span className="opp-card-title-extra">
          <Button
            type="button"
            variant="secondary"
            onClick={() => setOpen((v) => !v)}
            data-testid="opp-fulllink-toggle"
          >
            {open ? '收起' : '展开'}
          </Button>
        </span>
      </div>

      {!open ? (
        <div style={{ fontSize: 13, color: 'var(--rainier-color-text-3)' }}>
          展开以查看 商机 → 项目 → 运营 全链时间线。
        </div>
      ) : loading ? (
        <div style={{ fontSize: 13, color: 'var(--rainier-color-text-3)' }}>加载中…</div>
      ) : error ? (
        <div
          style={{ fontSize: 13, color: 'var(--rainier-status-red)' }}
          data-testid="opp-fulllink-error"
        >
          加载失败：{error}
        </div>
      ) : data ? (
        <div data-testid="opp-fulllink-body">
          <TimelineSegment
            kind="opportunity"
            title="商机"
            stages={data.presaleStages.concat(data.deliveryStages)}
            body={
              <div style={cellStyle}>
                <Row k="客户" v={data.opportunity.customerName} />
                <Row k="标题" v={data.opportunity.title} />
                <Row k="阶段" v={data.opportunity.stage} />
                <Row k="状态" v={data.opportunity.status} />
              </div>
            }
          />

          <TimelineSegment
            kind="project"
            title="项目（立项产物）"
            stages={[]}
            body={
              data.project ? (
                <div style={cellStyle}>
                  <Row k="编号" v={data.project.code} />
                  <Row k="名称" v={data.project.name} />
                  <Row k="状态" v={data.project.status} />
                  <div style={{ marginTop: 8 }}>
                    <Button
                      type="button"
                      variant="secondary"
                      onClick={() => navigate(`/pm/projects/${data.project!.id}`)}
                      data-testid="opp-fulllink-goto-project"
                    >
                      打开项目 ↗
                    </Button>
                  </div>
                </div>
              ) : (
                <div style={emptyStyle}>尚未立项</div>
              )
            }
          />

          <TimelineSegment
            kind="operation"
            title="运营（验收后接管）"
            stages={[]}
            body={
              data.operation ? (
                <div style={cellStyle}>
                  <Row k="标题" v={data.operation.title} />
                  <Row k="阶段" v={data.operation.stage} />
                  <Row k="状态" v={data.operation.status} />
                  <Row k="运营负责人" v={data.operation.opsOwnerName ?? '—'} />
                </div>
              ) : (
                <div style={emptyStyle}>尚未进入运营</div>
              )
            }
            isLast
          />

          <div style={{ marginTop: 12, fontSize: 12, color: 'var(--rainier-color-text-3)' }}>
            {data.customer
              ? `关联客户：${data.customer.name}${data.customer.industry ? ` · ${data.customer.industry}` : ''}`
              : '未关联客户实体'}
          </div>
        </div>
      ) : null}
    </div>
  );
}

const cellStyle: React.CSSProperties = {
  background: 'var(--rainier-bg-1)',
  border: '1px solid var(--rainier-border)',
  borderRadius: 'var(--rainier-radius-button)',
  padding: 12,
  fontSize: 13,
  display: 'grid',
  gap: 4,
};
const emptyStyle: React.CSSProperties = {
  background: 'var(--rainier-bg-1)',
  border: '1px dashed var(--rainier-border)',
  borderRadius: 'var(--rainier-radius-button)',
  padding: 12,
  fontSize: 13,
  color: 'var(--rainier-color-text-3)',
};

function Row({ k, v }: { k: string; v: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', gap: 8 }}>
      <span style={{ width: 80, color: 'var(--rainier-color-text-3)' }}>{k}</span>
      <span style={{ flex: 1, color: 'var(--rainier-color-text-1)' }}>{v}</span>
    </div>
  );
}

interface SegmentProps {
  kind: 'opportunity' | 'project' | 'operation';
  title: string;
  body: React.ReactNode;
  stages: FullLinkStageSummary[];
  isLast?: boolean;
}

function TimelineSegment({ kind, title, body, stages, isLast }: SegmentProps) {
  const accent =
    kind === 'opportunity'
      ? 'var(--rainier-color-primary)'
      : kind === 'project'
        ? 'var(--rainier-status-green)'
        : 'var(--rainier-status-yellow)';
  return (
    <div style={{ display: 'flex', gap: 12, paddingBottom: 16, position: 'relative' }}>
      <div
        style={{
          width: 14,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          paddingTop: 4,
        }}
      >
        <span
          style={{
            display: 'inline-block',
            width: 12,
            height: 12,
            borderRadius: '50%',
            background: accent,
            boxShadow: '0 0 0 2px var(--rainier-bg-2)',
          }}
        />
        {!isLast ? (
          <span
            style={{
              flex: 1,
              width: 2,
              background: 'var(--rainier-border)',
              marginTop: 4,
            }}
          />
        ) : null}
      </div>
      <div style={{ flex: 1 }}>
        <div
          style={{
            fontSize: 14,
            fontWeight: 600,
            color: accent,
            marginBottom: 6,
          }}
          data-testid={`opp-fulllink-${kind}-title`}
        >
          {title}
        </div>
        {body}
        {stages.length > 0 ? (
          <div
            style={{ display: 'flex', gap: 6, flexWrap: 'wrap', marginTop: 8 }}
            data-testid="opp-fulllink-stagechips"
          >
            {stages.map((s) => (
              <span
                key={s.code}
                title={`${s.label} · 活动 ${s.doneCount}/${s.activityCount}`}
                style={{
                  fontSize: 11,
                  padding: '2px 8px',
                  borderRadius: 999,
                  border: `1px solid ${s.current ? accent : 'var(--rainier-border)'}`,
                  background: s.current ? 'var(--rainier-bg-selected)' : 'transparent',
                  color: s.current ? accent : 'var(--rainier-color-text-2)',
                }}
              >
                {s.label}
                {s.activityCount > 0 ? ` ${s.doneCount}/${s.activityCount}` : ''}
              </span>
            ))}
          </div>
        ) : null}
      </div>
    </div>
  );
}
