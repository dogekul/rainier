# Test report — C4 richer-contribution-metrics (v0.0.84)

## Run

`mvn test` (full backend suite)

## Results

- Tests run: **740**, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
- New tests:
  - `ContributionMetricsServiceTest` — 5 cases (byStatus_grouped, thisWeek_counts,
    weeklyTrend_fourWeeks, weekStartUtc_isMondayMidnight, degraded_nullUserId_returnsZeroes)
  - `MeProfileControllerTest.profile_contributionPayload` — +1 case (existing 8 still green)
- Touched but unbroken: `UserProfileController` (reused; C3 subordinate path automatically carries
  the new contribution block via `MeProfileService.profileOfUserId`)

## Caveats

- `thisWeek_counts` relies on `AuditingEntityListener.createTime = now`. Robust within a single
  test run (`now` is always inside the current ISO week) but would need a clock seam for
  cross-week edge cases — out of scope for this change.
- `weekStartUtc` rolls week-based-year correctly via `IsoFields`, but the
  `weeklyTrend_fourWeeks` ordering assertion would fail across a week-based-year boundary where
  the label format produces e.g. "2026-W53" → "2027-W01"; the 4-week window in 2026 H2 is safe.
