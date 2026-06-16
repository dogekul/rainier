# Spec — frontend-scaffold (MODIFIED, v0.0.20)

> v0.0.20 ADDED: role-scoped navigation (isElevated), admin route guards, app-level me() hydration in
> ProtectedRoute, RolesPage adminAccess checkbox, WorkbenchPage reads store.

## Requirement: navigation is scoped by elevation

### Scenario: a non-admin user sees only the 工作台 and 需求管理 groups
- **GIVEN** an authenticated user whose roles all have `adminAccess = false`
- **WHEN** the AppLayout Sider renders
- **THEN** the `工作台` and `需求管理` groups SHALL be visible
- **AND** the `组织`, `产品`, `人事配置`, and `系统` groups SHALL NOT be rendered

### Scenario: an admin user sees all six groups
- **GIVEN** an authenticated user with at least one role `adminAccess = true`
- **WHEN** the AppLayout Sider renders
- **THEN** all six groups (工作台/组织/产品/需求管理/人事配置/系统) SHALL be visible

## Requirement: admin routes are guarded for non-admins

### Scenario: a non-admin navigating directly to an admin route is redirected home
- **GIVEN** an authenticated non-admin user with hydrated role context
- **WHEN** the user navigates to an admin route (e.g. `/org/users`, `/hr/roles`, `/sys/audit-logs`, `/pm/products`)
- **THEN** the router SHALL redirect to `/`

### Scenario: an admin navigating to an admin route is not redirected
- **GIVEN** an authenticated admin user with hydrated role context
- **WHEN** the user navigates to `/hr/roles`
- **THEN** the RolesPage SHALL render (no redirect)

### Scenario: a pm-group route stays open to non-admins
- **GIVEN** an authenticated non-admin user with hydrated role context
- **WHEN** the user navigates to `/pm/projects` (需求管理 group, all-users)
- **THEN** the ProjectsPage SHALL render (no redirect)

## Requirement: ProtectedRoute hydrates the current-user context app-wide

### Scenario: the current user is hydrated into the store once on entry
- **GIVEN** an authenticated user whose store `user` has no `roles`
- **WHEN** a protected route mounts
- **THEN** ProtectedRoute SHALL call `me()` once and write the result (id/name/roles/projects) into the auth store

### Scenario: an isElevated helper reflects any admin role
- **GIVEN** an AuthUser with roles `[{adminAccess:false},{adminAccess:true}]`
- **WHEN** `isElevated(user)` is evaluated
- **THEN** it SHALL return `true`
- **AND** for a user with no admin role it SHALL return `false`

## Requirement: RolesPage edits adminAccess

### Scenario: the role edit form exposes an adminAccess checkbox
- **GIVEN** the RolesPage 编辑/新建 drawer is open
- **WHEN** the user ticks the 「管理员权限」 checkbox and saves
- **THEN** the create/update request body SHALL carry `adminAccess: true`

## Requirement: WorkbenchPage reads the hydrated store

### Scenario: WorkbenchPage renders from the store-hydrated context
- **GIVEN** the auth store holds a user with id/name/roles/projects (hydrated by ProtectedRoute)
- **WHEN** WorkbenchPage renders
- **THEN** it SHALL show the greeting and roles from the store
- **AND** it SHALL load my-tasks/my-stories using the store user id
