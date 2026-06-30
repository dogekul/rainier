# Test Report

## Automated Tests

- RED: `cd backend && mvn -q -Dtest=RiskServicePushIntegrationTest test`
  - Failed with duplicate count `2`, expected `1`.
- RED: `cd backend && mvn -q -Dtest=RiskServicePushIntegrationTest,RiskScanSchedulerTest test`
  - Failed to compile because `RiskScanScheduler` did not exist.
- GREEN: `cd backend && mvn -q -Dtest=RiskServicePushIntegrationTest,RiskScanSchedulerTest test`
  - Passed.

## Coverage

- `TC-RDEDUP-001`: two consecutive scans of the same BLOCKED Story produce one unread CRIT notification.
- `TC-RSCHED-001`: scheduler scans enabled users and skips disabled users.

## Caveats

- Dedup fingerprint uses user, CRIT level, entity type/id, title, unread state, and suppression window. It does not persist a separate risk fingerprint column.
- Scheduler default scope is `mine`; broader PMO/company scans are left for a later policy pass.
