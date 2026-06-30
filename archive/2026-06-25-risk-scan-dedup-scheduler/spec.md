# Spec: Risk Scan Dedup Scheduler

## Scenario 1: Duplicate Suppression

Given a CRIT risk finding has already created an unread notification for a user
When `RiskService.runAll` sees the same rule/entity/title again within the suppression window
Then it must not create a second notification.

## Scenario 2: Read Notifications Can Resurface

Given a matching notification has been marked read
When the same unresolved risk is found again
Then the service may create a new notification, because the prior alert is no longer active in the unread queue.

## Scenario 3: Scheduled Scan

Given `app.risk.scan.enabled=true`
When the scheduler runs
Then it scans enabled users by calling `RiskService.runAll(loginName, scope)`
And skips disabled users and blank login names.

## Scenario 4: Test Profile Safety

Given the default `test` profile
Then scheduled risk scanning is disabled unless a test explicitly enables it with properties.
