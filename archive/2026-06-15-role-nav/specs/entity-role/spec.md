# Spec — entity-role (MODIFIED, v0.0.20)

> v0.0.20 ADDED: `Role.adminAccess` boolean flag (nullable column, read-coalesced null→false).

## Requirement: Role carries an adminAccess flag

### Scenario: creating a role without adminAccess defaults to false
- **GIVEN** a `POST /api/roles` body with code/name and no `adminAccess`
- **WHEN** the role is created
- **THEN** the response `adminAccess` SHALL be `false`
- **AND** the persisted row SHALL read `adminAccess = false`

### Scenario: creating a role with adminAccess=true persists true
- **GIVEN** a `POST /api/roles` body with `adminAccess: true`
- **WHEN** the role is created
- **THEN** the response `adminAccess` SHALL be `true`

### Scenario: updating a role toggles adminAccess
- **GIVEN** an existing role with `adminAccess = false`
- **WHEN** a `PUT /api/roles/{id}` body sets `adminAccess: true`
- **THEN** the updated role `adminAccess` SHALL be `true`

### Scenario: a legacy role row with NULL adminAccess reads as false
- **GIVEN** an existing `rainier_role` row whose `admin_access` column is NULL (pre-v0.0.20)
- **WHEN** the role is read via `GET /api/roles/{id}`
- **THEN** the response `adminAccess` SHALL be `false`
- **AND** no existing column value SHALL be mutated by the read
