# Test Report — project-organization-edge (H1, v0.0.108)

## 命令
```
cd backend && mvn test
```

## 结果
- **Backend**: Tests run: **885**, Failures: 0, Errors: 0, Skipped: 0
- 新增 6 个用例全绿；既有 879 全绿，无回归。

## 新增用例覆盖

### ProjectControllerOrgFilterTest（2）
- `getList_filterByOrganizationId_returnsOnlyMatching`：seed 3 项目（2×org=10, 1×org=20），
  `GET /api/projects?organizationId=10` → total=2 且 content[*].organizationId 全 ==10。
- `getList_noOrganizationIdParam_returnsAll`：不传 organizationId → total=3（既有行为不变）。

### ProjectOrgBackfillTest（4，@TestPropertySource flip 启用 flag）
- `run_walksUpToDepartment`：SUBGROUP→TEAM→DEPARTMENT 链 + alice 主组织=SG →
  backfill 后 P.organizationId == DEPT.id。
- `run_isIdempotent`：连跑两次写入值不变（只匹配 NULL 行）。
- `run_skipsNonNullRows`：预置 organizationId=999 的项目，backfill 后仍 ==999。
- `run_ownerHasNoPrimaryOrg_leavesNull`：bob 无任何 UserOrganization → P.organizationId 保持 null（不抛）。

## Caveats
- 当前 `ProjectService.list` 的 organizationId 过滤是精确等值，不展开子树；H2/H3 用到子树时再
  接 ScopeService 扩。
- backfill 仅基于 owner 主组织 + 上溯到 DEPARTMENT/DOMAIN/COMPANY；若 owner 主组织本身就在
  TEAM/SUBGROUP 且没有合格上级（如孤立树），该行下次启动重试。生产首次部署上线后建议查一次
  `SELECT count(*) FROM rainier_project WHERE organization_id IS NULL` 以排查孤儿数据。
- test profile 下 flag 默认 OFF（与 `ProjectMemberRoleBackfill` 等同款式），其他既有项目测试
  不受 backfill 干扰。
