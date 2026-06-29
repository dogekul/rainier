# Spec — project-organization-edge (H1, v0.0.108)

## Scenario A: GET /api/projects?organizationId=X 仅返回挂 X 的项目

GIVEN 3 个 Project：P1 organizationId=10、P2 organizationId=10、P3 organizationId=20
WHEN `GET /api/projects?organizationId=10`
THEN 200，content 长度 2，全部 organizationId==10

GIVEN 同上
WHEN `GET /api/projects`（不传 organizationId）
THEN 200，content 长度 3（既有列表行为不变）

## Scenario B: 启动回填 NULL organizationId

GIVEN flag `app.migration.project-org-backfill.enabled=true`
AND  Organization 树：DEPT(type=DEPARTMENT, parentId=null) → TEAM(type=TEAM, parentId=DEPT) → SG(type=SUBGROUP, parentId=TEAM)
AND  User alice 的 UserOrganization：organizationId=SG, isPrimary=true, leftAt=null
AND  Project P organizationId=null, ownerUserId=alice
WHEN `ProjectOrgBackfill.run()`
THEN P.organizationId == DEPT.id（沿 parentId 上溯到第一个 DEPARTMENT/DOMAIN/COMPANY）

## Scenario C: 回填幂等

GIVEN 上一轮回填后 P.organizationId 已写
WHEN 再次 `ProjectOrgBackfill.run()`
THEN P.organizationId 不变（只匹配 NULL 行）

## Scenario D: 非 NULL 不被回填

GIVEN Project Q organizationId=999（既存值）
WHEN `ProjectOrgBackfill.run()`
THEN Q.organizationId 仍 == 999

## Scenario E: owner 无主组织 → 留空

GIVEN Project R organizationId=null, ownerUserId=bob, bob 无任何 UserOrganization
WHEN `ProjectOrgBackfill.run()`
THEN R.organizationId 仍 null（下次启动重试，不抛）
