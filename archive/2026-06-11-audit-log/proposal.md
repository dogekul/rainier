# v0.0.15-audit-log — Spring AOP 切面自动审计实体写操作

## Why

管理员（角色卡 §卡17）需要「审计日志查询」——查清**谁、何时、对什么实体、做了什么**。现有 `BaseEntity` 的 `createBy/updateBy` 只存**当前快照**：答不出「这条需求被改过几次、第 3 次谁从 DRAFT 改到 ACTIVE」，软删后中间修改史全丢，也没法一条 SQL 查「alice 今天动过哪些东西」。本版加一条 **append-only 全量变更流水**补这个缺口，用 AOP 切面实现——**业务 service 代码零改动**——并提供前端审计查询页。

## What Changes

- **C1.** 加 `spring-boot-starter-aop` 依赖（pom.xml，当前未引入）。
- **C2.** NEW `entity-audit-log` capability：`AuditLog` 实体（append-only）—— 字段 `actor / entityType / entityId / action(CREATE|UPDATE|DELETE) / summary / createTime`。**无 update/delete API**（不可改不可删）。表 `rainier_audit_log`。
- **C3.** `AuditAspect`（`com.rainier.auditlog.aspect`）—— `@AfterReturning` 包住所有 `*Service.create/update/delete`（**A1 决策：约定式 pointcut，零注解**）：
  - **仅成功返回才记**（方法抛异常 → 不记）。
  - **同事务写入**（切面 @Order 配置在 `@Transactional` 内层；业务回滚 → 审计行一起回滚，不留「幽灵审计」）。
  - actor 复用 `AuditorAware<String>.getCurrentAuditor()`（"username" 或 "system"）。
  - **entityType** 从 service 类名推导（`RequirementService`→`REQUIREMENT`，`SprintFeatureLinkService`→`SPRINT_FEATURE_LINK`，camelCase→SCREAMING_SNAKE）。
  - **action** 从方法名推导（create/update/delete）。
  - **entityId**：create/update 从返回 `*Detail` 反射 `getId()`；delete 从首个 `Long` 参数。
  - **防自审计递归**：审计写入走 `AuditLogRepository.save()` 直连（不经 create() 方法），不匹配切点。
  - **A2 决策：不存 snapshot/字段级 diff** —— summary 先做 `"UPDATE REQUIREMENT#5"` 通用文本。
  - **A3 决策：全 17 service 全覆盖** —— 读操作（list/findById）不记。
- **C4.** 后端读 API：`GET /api/audit-logs?actor=&entityType=&entityId=&action=&page=&size=`，分页倒序（最新在前）+ `GET /api/audit-logs/{id}`。**无 POST/PUT/DELETE**。
- **C5.** 前端审计查询页 `AuditLogsPage`：表格（actor / entityType / entityId / action / 时间）+ 过滤（actor / entityType / entityId / action）+ 分页 + 新路由 + Sider 入口（具体分组 Phase 2 定，倾向新增「系统」顶级组或并入「人事配置」）。`api/auditLog.ts`。**只读页，无新建/编辑/删除按钮**。

## Capabilities

### New Capabilities

- `entity-audit-log` —— AuditLog append-only 实体 + 读 API + AuditAspect 横切切面

### Modified Capabilities

- `frontend-scaffold` —— 新增 AuditLogsPage（只读查询）+ 路由 + Sider 入口

> 注：C1 pom 依赖 + C3 切面是横切基建，**不改任何现有 service/controller 业务行为**。

## Impact

**代码层面**：
- 新增 `com.rainier.auditlog.*`：domain/AuditLog + AuditAction(常量) + repository + service(read-only 查询 + `record` 内部方法) + controller + aspect/AuditAspect ≈ 6-7 文件。
- 新增后端测试：AuditAspect 集成测试（create/update/delete 各产生 1 行 + actor + 失败不记 + 同事务回滚不留行 + 复合名 entityType + 防递归）+ AuditLogController 查询测试 + perf。
- 新增前端：`api/auditLog.ts` + `pages/AuditLog/{AuditLogsPage.tsx,index.tsx}` + AppRoutes 路由 + AppLayout Sider 入口 + AuditLogsPage 测试 ≈ 5 文件。
- **零现有 service/controller 文件改动**（AOP 横切）；AppRoutes/AppLayout 各加 1 项。
- pom.xml +1 依赖。

**配置层面**：
- `spring-boot-starter-aop` 依赖。
- 切面 @Order 常量（确保事务内层）。

**基础设施**：
- 17 → **18 张表**（+`rainier_audit_log`）。
- 不动任何现有表 / 数据（纯新增，satisfies standing 约束）。

## 显式排除（推到后续）

- 字段级 before/after diff / snapshot（A2 已定不存）
- AI 错误公示板（飞轮 §5.7，AI 层）
- 跨层一致性审计（飞轮 §4.7，derived 层）
- 审计保留期 / 归档 / 清理策略
- 审计读 API / 前端页的权限收口（仅管理员可见）—— 先做端点与页，权限后续
- 异步写审计（先同事务同步，性能够用）

## Success Criteria

- [ ] `spring-boot-starter-aop` 在 pom，应用启动加载 AspectJ。
- [ ] `SHOW TABLES` 含 `rainier_audit_log`，共 18 张表。
- [ ] 任一实体 `POST /api/X`（成功）→ 产生 1 条 audit_log，`action=CREATE`、`entityType` 正确、`entityId` = 新建 id、`actor` 正确。
- [ ] `PUT /api/X/{id}`（成功）→ `action=UPDATE`、`entityId={id}`。
- [ ] `DELETE /api/X/{id}`（成功）→ `action=DELETE`、`entityId={id}`。
- [ ] 业务方法抛异常（如 409 重复 code）→ **不产生** audit_log 行（@AfterReturning 只记成功）。
- [ ] 业务事务回滚 → 审计行**一起回滚**（同事务，不留幽灵行）。
- [ ] `GET /api/audit-logs?entityType=REQUIREMENT&entityId=5` 返回该实体的变更历史，倒序。
- [ ] audit_log **无** POST/PUT/DELETE 端点（append-only）。
- [ ] entityType 推导对复合名正确（SprintFeatureLinkService → `SPRINT_FEATURE_LINK`）。
- [ ] 审计写入不自我递归（写 audit_log 自身不再产生 audit_log）。
- [ ] 前端 `AuditLogsPage` 渲染审计表格 + 过滤（actor/entityType/entityId/action）+ 分页；无新建/编辑/删除按钮。
- [ ] 前端 Sider 有审计日志入口，路由直接访问渲染该页。
- [ ] 全量 backend + frontend 测试 green（293+54 baseline + 新增）；E2E：建实体 → 查 audit_log 见对应行 → 现有 17 表数据未改。
- [ ] standing 约束：现有实体数据零改动。
