import { statusColor, type StatusTier } from '../../utils/board';
import './board.css';

export interface StatusChipProps {
  status: string;
  /** Display label (defaults to the raw status). */
  label?: string;
  /** v0.0.29: pin the color tier directly (e.g. an RYG verdict) instead of deriving from status. */
  tier?: StatusTier;
  testId?: string;
}

/** v0.0.22 board-kit — a small tinted chip colored by the status' tier (or an explicit tier). */
export function StatusChip({ status, label, tier: tierProp, testId }: StatusChipProps) {
  const tier = tierProp ?? statusColor(status);
  return (
    <span
      className="rainier-status-chip"
      data-testid={testId ?? 'status-chip'}
      data-tier={tier}
      style={{
        color: `var(--rainier-status-${tier})`,
        background: `var(--rainier-status-${tier}-bg)`,
      }}
    >
      {label ?? status}
    </span>
  );
}
