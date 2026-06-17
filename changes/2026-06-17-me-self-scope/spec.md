# Spec — me-self-scope (NEW backend slice, v0.0.24)

## Requirement: a user can read the teams they lead

### Scenario: HEAD of one team
- **GIVEN** alice is an active HEAD (role=HEAD, leftAt null) of team T
- **WHEN** `GET /api/me/led-teams` with alice's token
- **THEN** the response SHALL contain one item with `organizationId=T`, its name and `organizationType=TEAM`

### Scenario: HEAD of nothing
- **GIVEN** alice heads no team
- **WHEN** `GET /api/me/led-teams`
- **THEN** the response SHALL be `[]`

### Scenario: MEMBER role does not count as leading
- **GIVEN** bob is a MEMBER (not HEAD) of team T
- **WHEN** `GET /api/me/led-teams` with bob's token
- **THEN** the response SHALL be `[]`

## Requirement: a HEAD can read their team's active members, others cannot

### Scenario: HEAD reads active members
- **GIVEN** alice HEADs team T with active members bob, carol and a left (leftAt set) member gone
- **WHEN** `GET /api/me/team-members?organizationId=T` with alice's token
- **THEN** the response SHALL contain alice, bob, carol (active) and SHALL NOT contain gone

### Scenario: non-HEAD is refused
- **GIVEN** bob is a MEMBER (not HEAD) of team T
- **WHEN** `GET /api/me/team-members?organizationId=T` with bob's token
- **THEN** the server SHALL return 403

### Scenario: missing token
- **GIVEN** no Authorization header
- **WHEN** `GET /api/me/led-teams`
- **THEN** the server SHALL return 401
