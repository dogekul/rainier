# Spec — auth-baseline (backend-authz/auth-placeholder MOD, v0.0.27)

## Requirement: every /api/** requires a valid token when the baseline is enabled

### Scenario: missing token on an all-users endpoint
- **GIVEN** require-all-users-token=true and no Authorization header
- **WHEN** `GET /api/projects`
- **THEN** the filter SHALL return 401 with body `{"message":"Missing or invalid token"}` before any controller

### Scenario: valid token passes identity
- **GIVEN** require-all-users-token=true and a valid Bearer token
- **WHEN** `GET /api/projects`
- **THEN** it SHALL NOT be 401 (request proceeds; rainier.username is set)

### Scenario: invalid token
- **GIVEN** require-all-users-token=true and an unparseable Bearer token
- **WHEN** `GET /api/tasks`
- **THEN** it SHALL return 401

### Scenario: whitelist
- **GIVEN** require-all-users-token=true
- **WHEN** `GET /api/health` or `POST /api/auth/login` without a token
- **THEN** it SHALL NOT be gated (no 401)

## Requirement: identity precedes authorization

### Scenario: no token on an admin endpoint → 401 not 403
- **GIVEN** require-all-users-token=true AND admin-authz=true, no token
- **WHEN** `GET /api/roles`
- **THEN** it SHALL return 401 (SecurityFilter), not 403

### Scenario: non-admin token on an admin endpoint → 403
- **GIVEN** both flags true and a valid token for a non-elevated user
- **WHEN** `GET /api/roles`
- **THEN** identity SHALL pass and the interceptor SHALL return 403

## Requirement: flag-off preserves legacy behavior

### Scenario: test profile default
- **GIVEN** require-all-users-token=false (test default)
- **WHEN** any existing unauthenticated mockMvc request runs
- **THEN** it SHALL behave exactly as before (no new 401)
