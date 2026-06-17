# Spec — team-lead (frontend-scaffold MOD, v0.0.25)

## Requirement: rule-based health + load (pure)

### Scenario: project RYG by rules
- **GIVEN** ryg inputs
- **WHEN** openCount=0 → gray; anyBlocked → red; overdueRatio>0.3 → red; 0<ratio<=0.3 → yellow; ratio=0 → green
- **THEN** it SHALL return that tier; RYG_ORDER sorts red<yellow<green<gray

### Scenario: member load band
- **GIVEN** loadTier(openTasks)
- **WHEN** <=3 → green; 4-7 → yellow; >7 → red
- **THEN** it SHALL return that tier

## Requirement: the panel scopes to the teams the user leads

### Scenario: sole team auto-selected
- **GIVEN** the user HEADs exactly one team
- **WHEN** /team renders
- **THEN** that team SHALL be selected with no dropdown, and member rows SHALL render

### Scenario: leading no team
- **GIVEN** /api/me/led-teams returns []
- **WHEN** /team renders
- **THEN** it SHALL show '你当前不是任何团队的负责人' and no cards

## Requirement: member load + project ranking with drill-through

### Scenario: member load colored and drillable
- **GIVEN** a member with 5 open tasks
- **WHEN** the 成员负载 card renders
- **THEN** the member's load bar SHALL be yellow and the row SHALL link to /pm/tasks?assigneeUserId={id}

### Scenario: projects ranked red→gray
- **GIVEN** project A with overdueRatio 0.4 and project B with no open tasks
- **WHEN** the 项目红黄绿 card renders
- **THEN** A SHALL be red and sorted before B (gray), each linking to /pm/tasks?projectId={id}
