# Test Report — pmo-company-map (H3, v0.0.110)

## Backend
- `cd backend && mvn test` → 896 tests, 0 failures, 0 errors, 0 skipped, BUILD SUCCESS
- New: `PmoPortfolioControllerTest` 5/5
  - TC-PMO-01 groupBy=organization slices & RYG counts (worst-first by red)
  - TC-PMO-02 groupBy=owner slices by ownerUserId, group.type=USER
  - TC-PMO-03 organizationId NULL → group "未归属"
  - TC-PMO-04 groupBy=none → single "全公司" group
  - TC-PMO-05 no token → 401

## Frontend
- `cd frontend && npm test -- --run` → 64 files, 312 tests, all green
- New: `src/pages/Pmo/PmoPortfolioPage.test.tsx` 2/2
  - TC-PMOFE-01 default organization-grouped cards + RYG chips + project rows
  - TC-PMOFE-02 toggle to owner triggers refetch with new dimension

## Coverage notes
- No regression to portfolio/scope tests (PortfolioControllerTest, ScopeServiceFootprintTest).
- AppRoutes + AppLayout nav-consistency tests still pass (navGuardConsistency 34/34).
- DashboardCard prop is `extra` (not `actions`); RYG chips rendered in header extra slot.
