import { statusColor, type StatusTier } from '../../utils/board';
import './board.css';

export interface StatusBarSegment {
  label: string;
  count: number;
  /** Resolve color from an entity status (via statusColor)… */
  status?: string;
  /** …or pin the tier explicitly (e.g. a threshold-derived load color). */
  tier?: StatusTier;
}

export interface StatusBarProps {
  segments: StatusBarSegment[];
  /** Denominator for the fills. When set, widths are count/max (a ratio bar); else count/sum (a distribution). */
  max?: number;
  showLegend?: boolean;
  testId?: string;
}

function segTier(s: StatusBarSegment): StatusTier {
  return s.tier ?? (s.status ? statusColor(s.status) : 'gray');
}

/**
 * v0.0.22 board-kit — a plain CSS stacked bar (no chart lib). Two modes: distribution (widths sum to
 * 100% of the total counts) or ratio (widths are count/max). Renders a placeholder when empty.
 */
export function StatusBar({ segments, max, showLegend = true, testId }: StatusBarProps) {
  const total = segments.reduce((a, s) => a + Math.max(0, s.count), 0);
  const denom = max ?? total;
  const tid = testId ?? 'statusbar';

  if (denom <= 0) {
    return (
      <div className="rainier-statusbar-empty" data-testid={`${tid}-empty`}>
        —
      </div>
    );
  }

  return (
    <div data-testid={tid}>
      <div className="rainier-statusbar-track" role="img" aria-label="状态分布">
        {segments
          .filter((s) => s.count > 0)
          .map((s, i) => {
            const tier = segTier(s);
            return (
              <div
                key={`${s.label}-${i}`}
                className="rainier-statusbar-fill"
                data-testid={`${tid}-fill-${s.label}`}
                data-tier={tier}
                title={`${s.label}: ${s.count}`}
                style={{
                  width: `${(s.count / denom) * 100}%`,
                  background: `var(--rainier-status-${tier})`,
                }}
              />
            );
          })}
      </div>
      {showLegend && (
        <div className="rainier-statusbar-legend">
          {segments
            .filter((s) => s.count > 0)
            .map((s, i) => {
              const tier = segTier(s);
              return (
                <span
                  key={`${s.label}-${i}`}
                  className="rainier-statusbar-legend-item"
                  data-testid={`${tid}-legend-${s.label}`}
                >
                  <span
                    className="rainier-statusbar-dot"
                    style={{ background: `var(--rainier-status-${tier})` }}
                    aria-hidden="true"
                  />
                  {s.label} {s.count}
                </span>
              );
            })}
        </div>
      )}
    </div>
  );
}
