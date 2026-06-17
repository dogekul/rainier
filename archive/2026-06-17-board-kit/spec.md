# Spec — board-kit (frontend-scaffold MOD, v0.0.22)

## Requirement: status color tier mapping

### Scenario: each entity status maps to the intended tier
- **GIVEN** the `statusColor` util
- **WHEN** called with DONE/DELIVERED/REACHED/COMPLETED → green; BLOCKED/MISSED → red; DRAFT/CANCELLED/CLOSED/ARCHIVED → gray; any other (TODO/IN_PROGRESS/ACTIVE/PLANNING/READY/IN_APPROVAL…) → yellow; null/unknown → gray
- **THEN** it SHALL return that tier

## Requirement: aggregation utils are pure and correct

### Scenario: groupByStatus counts per status in first-seen order
- **GIVEN** items with statuses [TODO,DONE,TODO,BLOCKED]
- **WHEN** `groupByStatus(items, getStatus)`
- **THEN** it SHALL return [{TODO:2},{DONE:1},{BLOCKED:1}] in that order

### Scenario: isOverdue is a pure date compare
- **GIVEN** `isOverdue(dateStr, today)`
- **WHEN** dateStr < today → true; dateStr >= today → false; dateStr null/empty → false
- **THEN** it SHALL return that boolean (comparing the YYYY-MM-DD prefix)

## Requirement: StatusBar renders a proportional distribution

### Scenario: segments render proportional fills
- **GIVEN** segments [{label:TODO,count:3,status:TODO},{label:DONE,count:1,status:DONE}]
- **WHEN** StatusBar renders
- **THEN** it SHALL render one fill per non-zero segment with width = count/total% and the tier color
- **AND** a legend showing each label + count

### Scenario: empty distribution shows a placeholder
- **GIVEN** segments summing to 0 (or empty)
- **WHEN** StatusBar renders
- **THEN** it SHALL render an empty placeholder (testid `statusbar-empty`), not a zero-width bar

## Requirement: dashboard primitives render

### Scenario: DashboardCard shows title + extra + children
- **GIVEN** DashboardCard title="任务" extra=<X/>
- **WHEN** rendered
- **THEN** title, extra, and children SHALL be present

### Scenario: EmptyState fires its single CTA
- **GIVEN** EmptyState message + cta {label,onClick}
- **WHEN** the CTA button is clicked
- **THEN** onClick SHALL fire once

### Scenario: StatusChip / OwnerChip render label + initial
- **GIVEN** StatusChip status=BLOCKED, OwnerChip name="黎立"
- **WHEN** rendered
- **THEN** StatusChip SHALL carry the red tier; OwnerChip SHALL show initial "黎" + name
