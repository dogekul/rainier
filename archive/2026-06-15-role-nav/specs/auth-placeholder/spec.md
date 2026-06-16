# Spec — auth-placeholder (MODIFIED, v0.0.20)

> v0.0.20 ADDED: each `GET /api/auth/me` role assignment carries `adminAccess`.

## Requirement: me() role assignments expose adminAccess

### Scenario: a user whose role has adminAccess=true sees it in me()
- **GIVEN** a user assigned a role whose `adminAccess` is true
- **WHEN** `GET /api/auth/me` is called with that user's token
- **THEN** the matching `roles[].adminAccess` SHALL be `true`

### Scenario: a user whose role has adminAccess=false (or NULL) sees false
- **GIVEN** a user assigned a role whose `adminAccess` is false or a legacy NULL
- **WHEN** `GET /api/auth/me` is called
- **THEN** the matching `roles[].adminAccess` SHALL be `false`
- **AND** the field SHALL never be null in the response
