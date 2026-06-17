import './board.css';

export interface OwnerChipProps {
  name?: string | null;
  loginName?: string | null;
  testId?: string;
}

/**
 * v0.0.22 board-kit — an avatar-initial circle + name, the standard way to show a person across the
 * dashboards. Falls back to loginName, then a neutral placeholder.
 */
export function OwnerChip({ name, loginName, testId }: OwnerChipProps) {
  const display = name || loginName || '未分配';
  const initial = (name || loginName || '?').trim().charAt(0).toUpperCase();
  const assigned = Boolean(name || loginName);
  return (
    <span className="rainier-owner-chip" data-testid={testId ?? 'owner-chip'}>
      <span className="rainier-owner-avatar" aria-hidden="true" data-assigned={assigned}>
        {initial}
      </span>
      <span className="rainier-owner-name">{display}</span>
    </span>
  );
}
