# v0.0.15-audit-log 任务清单

## 1. 基建（P0）

- [x] 1.1 `backend/pom.xml` 加 `spring-boot-starter-aop`
- [x] 1.2 `AuditTxConfig`（@Configuration @EnableTransactionManagement(order=Ordered.HIGHEST_PRECEDENCE)）

## 2. entity-audit-log 骨架（P0）

- [x] 2.1 `AuditLog`（extends BaseEntity；actor / entity_type / entity_id / action / summary；表 rainier_audit_log；无 @SQLDelete）
- [x] 2.2 `AuditAction` 常量（CREATE / UPDATE / DELETE）
- [x] 2.3 `AuditLogRepository`（JpaRepository + JpaSpecificationExecutor）
- [x] 2.4 `AuditLogDetail` DTO（id/actor/entityType/entityId/action/summary/createTime + from()）（依赖 #2.1）

## 3. AuditLogService（P0）

- [x] 3.1 `record(actor, entityType, entityId, action, summary)` → save（命名避开 create）（依赖 #2.3）
- [x] 3.2 `query(actor, entityType, entityId, action, PageParams)` → PageResponse 倒序（Specification）（依赖 #2.3）
- [x] 3.3 `findById(Long)` → AuditLogDetail（依赖 #2.4）

## 4. AuditAspect（P0）

- [x] 4.1 `@Aspect` + 3 `@Pointcut`（execution * com.rainier..*Service.create/update/delete）（依赖 #1.1）
- [x] 4.2 `@AfterReturning`：注入 AuditorAware<String> + AuditLogRepository（直连 save，防自审计）（依赖 #3.1, #1.2）
- [x] 4.3 entityType：`getDeclaringType().getSimpleName()` 去 Service + camelCase→SCREAMING_SNAKE（陷阱 C/F）（依赖 #4.1）
- [x] 4.4 action：方法名 toUpperCase（依赖 #4.1）
- [x] 4.5 entityId：create/update 反射 returnValue.getId()；delete (Long)args[0]；try/catch 兜底 null+WARN（陷阱 E）（依赖 #4.1）
- [x] 4.6 summary = action+" "+entityType+"#"+entityId（依赖 #4.3, #4.4, #4.5）

## 5. AuditLogController（P0）

- [x] 5.1 `@RequestMapping("/api/audit-logs")` GET list（actor/entityType/entityId/action/page/size）+ GET /{id}；无 POST/PUT/DELETE（依赖 #3.2, #3.3）

## 6. backend 测试（P0）

- [x] 6.1 `AuditAspectIntegrationTest`：TC-AUD-001..004（create/update/delete 各记 + summary）+ 005（失败不记）+ 006（同事务回滚）+ 007（复合名 SPRINT_FEATURE_LINK）+ 008（防自递归）+ 009（多 service 抽样）；actor 注入（陷阱 K）（依赖 #4.6, #5.1）
- [x] 6.2 `AuditLogControllerQueryTest`：TC-AUD-010..014（entityType+id 过滤 / actor 过滤 / 倒序 / 单查 / append-only 无写端点）（依赖 #5.1）
- [x] 6.3 perf：TC-PERF-AUD-001（写路径审计 +1 INSERT 有界）+ TC-PERF-AUD-002（list ≥2∧≤3）（依赖 #4.6, #5.1）

## 7. 前端 api + 页（P0）

- [x] 7.1 `api/auditLog.ts`：AuditLog 类型 + listAuditLogs(params) + getAuditLog（依赖 #5.1）
- [x] 7.2 `AuditLogsPage`：只读 Table + Pagination + 过滤（actor/entityType/entityId/action）；无新建/编辑/删除按钮 + index.tsx（依赖 #7.1）

## 8. 前端 路由 + Sider + 测试（P0）

- [x] 8.1 `AppRoutes` +/sys/audit-logs → AuditLogsPage（依赖 #7.2）
- [x] 8.2 `AppLayout` +「系统」第 5 顶级组（key sys，末位，含审计日志→/sys/audit-logs）（依赖 #7.2）
- [x] 8.3 `AppLayout.test` 既有「4 顶级组」断言改 5 + 加 TC-FES-AUD-001（陷阱 H）（依赖 #8.2）
- [x] 8.4 `AuditLogsPage.test`（TC-FES-AUD-002 渲染+只读 / 003 过滤触发查询）+ AppRoutes grep（TC-FES-AUD-004）（依赖 #8.1, #7.2）

## 9. 测试与验证（P0）

- [x] 9.1 全量 backend `mvn test`（预期 ≈ 311）
- [x] 9.2 全量 frontend `npx vitest run` + `tsc --noEmit`（预期 ≈ 58）
- [x] 9.3 E2E：docker 重建 + SHOW TABLES=18（含 rainier_audit_log）+ curl 建 requirement → 查 audit-logs 见 CREATE REQUIREMENT 行（TC-E2E-AUD-001）
- [x] 9.4 E2E：既有 17 表数据未被审计逻辑改动（TC-E2E-AUD-002）
