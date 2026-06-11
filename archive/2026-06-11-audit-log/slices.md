# v0.0.15-audit-log 切片执行计划

> 9 切片全 P0。M01(基建)→M02(骨架)→M03(service)→M04(切面，依赖 M03+M01 同事务配置)→M05(controller)→M06(测试)。前端 M07→M08。E2E M09。

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|--------|---------|---------|------|
| M01 | P0 | (基建) | pom +spring-boot-starter-aop + `AuditTxConfig`(@EnableTransactionManagement(order=HIGHEST_PRECEDENCE)) | 无 |
| M02 | P0 | (结构) | `AuditLog`(extends BaseEntity, actor/entityType/entityId/action/summary) + `AuditAction` 常量 + `AuditLogRepository` + `AuditLogDetail` | 无 |
| M03 | P0 | (服务) | `AuditLogService`: record(写) + query(分页倒序+过滤) + findById；命名避开 create/update/delete | M02 |
| M04 | P0 | (切面) | `AuditAspect`: 3 pointcut + @AfterReturning；entityType(去Service+SNAKE)/action/entityId(反射 getId / 首参)/summary；防自审计；反射兜底 | M01, M03 |
| M05 | P0 | (端点) | `AuditLogController` /api/audit-logs GET list + GET /{id}；无写端点 | M03 |
| M06 | P0 | TC-AUD-001..014 + PERF×2 | AuditAspectIntegrationTest(9) + AuditLogControllerQueryTest(5) + perf(2) | M04, M05 |
| M07 | P0 | (api+页) | api/auditLog.ts + AuditLogsPage(只读) + index.tsx | M05 |
| M08 | P0 | TC-FES-AUD-001..004 | AppRoutes /sys/audit-logs + AppLayout「系统」第 5 组 + AppLayout.test 4→5 + AuditLogsPage.test + grep | M07 |
| M09 | P0 | TC-E2E-AUD-001/002 | docker 重建 + SHOW TABLES=18 + curl 审计链 + 既有数据未改 | M01..M08 |

## 执行批次（拓扑序）

```
批次 1（可并行）: M01, M02
批次 2: M03 (← M02)
批次 3: M04 (← M01,M03), M05 (← M03)
批次 4: M06 (← M04,M05), M07 (← M05)
批次 5: M08 (← M07)
批次 6: M09 (← 全部)
```

## 隐藏陷阱备忘（from Phase 2 + 经验）

- **A** Java 8: 无 `Set.of`/`List.of`/无参 `orElseThrow()`/`var`。
- **B（核心）** 切面同事务: `@EnableTransactionManagement(order=Ordered.HIGHEST_PRECEDENCE)` 让 tx 最外层 + AuditAspect 默认 order 内层。TC-AUD-006 用回滚路径断言审计行不存在。底线: @AfterReturning 只记成功（TC-AUD-005 失败不记必过）。
- **C** CGLIB 代理名: 用 `getSignature().getDeclaringType().getSimpleName()`，非 `getTarget().getClass()`。
- **D** 防自审计: AuditLogService 方法名 `record/query/findById`，绝不叫 create/update/delete；切面走 `AuditLogRepository.save()` 直连。
- **E** 反射 getId 兜底: try/catch 包 reflection，异常 entityId=null + `log.warn`，不抛不阻断业务。
- **F** entityType 正则: `replaceAll("([a-z0-9])([A-Z])","$1_$2").toUpperCase()`；`SprintFeatureLink`→`SPRINT_FEATURE_LINK` 验证。
- **G** 切点范围: 仅 `*Service.create/update/delete`；AuditLogService.query 不匹配（名不同）；bootstrap runner 非 *Service 不误伤。
- **H** Sider 4→5: 既有 AppLayout.test「4 顶级组」断言（v0.0.13 TC-FES-PROD-001）同步改 5；grep 校对其它组断言不动。
- **I** perf 范围断言防 statistics 假绿。
- **J** AuditLog extends BaseEntity append-only: 无 @SQLDelete；createTime 做时间戳；actor 独立列（切面写）非复用 createBy。
- **K** 测试 actor 注入: mock request attr "username"；既有测试 actor 默认 "system"（unauthenticated）。actor=alice 的测试需注入 username。
