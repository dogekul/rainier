import type { StatusTier } from '../../utils/board';
import './board.css';

export interface StatTile {
  label: string;
  value: number | string;
  /** Optional status tier — tints the number (e.g. red for 逾期). */
  tier?: StatusTier;
}

export interface StatTilesProps {
  tiles: StatTile[];
  testId?: string;
}

/**
 * v0.0.37 board-kit — a row of KPI stat tiles (big number + label), the 飞书项目 / Linear dashboard
 * summary pattern. Shared by the workbench, portfolio, and panels so headline numbers read identically.
 */
export function StatTiles({ tiles, testId }: StatTilesProps) {
  return (
    <div className="rainier-stat-tiles" data-testid={testId}>
      {tiles.map((t, i) => (
        <div key={i} className="rainier-stat-tile" data-testid={testId ? `${testId}-${i}` : undefined}>
          <div
            className="rainier-stat-num"
            style={t.tier ? { color: `var(--rainier-status-${t.tier})` } : undefined}
          >
            {t.value}
          </div>
          <div className="rainier-stat-label">{t.label}</div>
        </div>
      ))}
    </div>
  );
}
