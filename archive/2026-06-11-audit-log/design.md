# v0.0.15-audit-log — 技术设计

## Context

**Baseline**：tag `v0.0.14-sprint-feature-link` / commit `717ae99`。293 backend + 54 frontend tests green，17 张表。

**现有基建**：
- `BaseEntity`：id(auto) / createBy / createTime / updateBy / updateTime / del_flag。提供「当前快照」审计字段，无变更历史、无删除事件记录。
- `AuditorAwareImpl implements AuditorAware<String>`：`getCurrentAuditor()` 读 request attr `"username"`，无则 `"system"`。Spring Data JPA `@CreatedBy/@LastModifiedBy` 用它填 createBy/updateBy。
- 全 17 entity service 写方法命名统一：`create(req) → XxxDetail`（有 `getId()`）/ `update(Long id, req) → XxxDetail` / `delete(Long id) → void`。
- `spring-boot-starter-aop` **未引入**。
- Spring Boot 2.7 auto-config 默认启用事务管理（无显式 `@EnableTransactionManagement`），tx interceptor order = `Ordered.LOWEST_PRECEDENCE`（最内层）。
- Sider 现 4 顶级组：组织 / 产品 / 需求管理 / 人事配置。

**约束**：Java 8；ddl-auto=update（dev/docker）/ create-drop（H2 test）；standing 约束「不删改已有数据」——本版纯新增（新表 + 新切面），零现有数据改动。

---

## Decisions

### 1. AuditLog 实体 —— extends BaseEntity，append-only，显式 actor 列

**方案**：`AuditLog extends BaseEntity`，新增列 `actor` / `entity_type` / `entity_id` / `action` / `summary`。审计时间戳直接用 BaseEntity 的 `createTime`。`updateBy/updateTime/del_flag` 继承但不用（无更新、无删除）。**无 `@SQLDelete`**（但也无 delete API → API 契约上 append-only）。

**为什么**：与家族模式一致（DemandRequirementLink 同样 extends BaseEntity 留 del_flag 不用）；createTime 现成做时间戳。

**actor 为何独立列而非复用 createBy**：createBy 由 JPA 审计在 persist 时填（同样来自 AuditorAwareImpl，值相同），但语义上 `actor` 是「执行被审计动作的人」，由切面显式捕获写入——独立列让审计语义自解释、与 JPA 审计基建解耦。

**备选**：不继承 BaseEntity、纯手写 id+createTime —— 放弃，破坏家族一致性。

### 2. 约定式 pointcut（A1，零注解）

**方案**：
```java
@Pointcut("execution(public * com.rainier..*Service.create(..))")  void create() {}
@Pointcut("execution(public * com.rainier..*Service.update(..))")  void update() {}
@Pointcut("execution(public * com.rainier..*Service.delete(..))")  void delete() {}
```
匹配 `com.rainier` 下任意 `*Service` 的 create/update/delete。

**为什么**：零 service 改动（用户精简偏好）；命名约定已被全 17 service 100% 遵循（Phase 1 grep 确认）。

**防自审计**：`AuditLogService` 的方法命名为 `query/findById`（**不叫 create/update/delete**）→ 不匹配切点。切面写审计走 `AuditLogRepository.save()` 直连，不经任何 create() → 无递归。

**备选**：`@Audited` 注解（~51 处）—— 更显式但 touch 面大，与精简偏好相悖，排除。

### 3. entityType 从声明类名推导（camelCase → SCREAMING_SNAKE）

**方案**：
```java
String simple = joinPoint.getSignature().getDeclaringType().getSimpleName(); // 用声明类型避免 CGLIB 代理名
String base = simple.endsWith("Service") ? simple.substring(0, simple.length()-7) : simple;
String entityType = base.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toUpperCase();
```
`RequirementService`→`REQUIREMENT`；`SprintFeatureLinkService`→`SPRINT_FEATURE_LINK`；`UserOrganizationService`→`USER_ORGANIZATION`。

**为什么**：用 `getDeclaringType()`（非 `getTarget().getClass()`）拿到声明类而非 CGLIB 代理类名；正则在小写/数字→大写边界插下划线。

### 4. action 从方法名推导

**方案**：`joinPoint.getSignature().getName().toUpperCase()` → `CREATE` / `UPDATE` / `DELETE`（仅这三个切点，不会有其它值）。

### 5. entityId 解析 —— create/update 反射 getId()，delete 取首参

**方案**：
```java
if (action == DELETE) {
  entityId = (Long) joinPoint.getArgs()[0];           // delete(Long id)
} else {
  Object ret = returnValue;                            // create/update → *Detail
  entityId = (Long) ret.getClass().getMethod("getId").invoke(ret);
}
```

**为什么**：所有 `*Detail` 有 `public Long getId()`；delete 首参恒为 Long id。反射避免给所有 DTO 加公共接口（零 touch）。失败兜底：反射异常时 entityId 置 null + 记 WARN（不阻断业务）。

### 6. 同事务写入 —— @EnableTransactionManagement(order = HIGHEST_PRECEDENCE) 让 tx 最外层

**方案**：新增 `@Configuration @EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)`，使事务 advice 成为**最外层**；`AuditAspect`（默认 order，内层）的 `@AfterReturning` 在事务**仍开启**时执行，`auditLogRepository.save()` 以 PROPAGATION_REQUIRED 加入当前业务事务。业务回滚 → 审计行随之回滚。

**为什么**：Spring 默认 tx = `LOWEST_PRECEDENCE`（最内层）会让切面跑在 tx 外、审计落到独立事务；把 tx 提到最外层即让审计嵌在业务事务内，实现原子性。当前无其它自定义切面，提升 tx order 无副作用。

**备选**：保持默认 + 审计独立事务 —— 业务提交后审计单独写，失去原子性，排除。

### 7. 仅 @AfterReturning 成功记录

**方案**：用 `@AfterReturning(pointcut, returning="result")`。业务方法**抛异常则 advice 不触发** → 不记审计。

**为什么**：失败的写操作不应留审计；@AfterReturning 天然只在正常返回时执行，无需手动 try/catch。

### 8. summary 文本格式（A2 — 不存 diff）

**方案**：`summary = action + " " + entityType + "#" + entityId`，如 `"UPDATE REQUIREMENT#5"`。

**为什么**：v0.0.15 不做字段级 before/after（A2 决策）；通用文本够管理员定位「谁动了哪条」。字段 diff 留待合规需求。

### 9. 读 API —— 分页倒序 + 多过滤，无写端点

**方案**：`AuditLogController`：
- `GET /api/audit-logs?actor=&entityType=&entityId=&action=&page=&size=` → PageResponse，按 createTime DESC。
- `GET /api/audit-logs/{id}` → 单条。
- **无 POST/PUT/DELETE**。
过滤用 JPA Specification（与既有 list 端点同款）。

### 10. 审计自身的读操作不被审计

**方案**：AuditLogController 是 GET；AuditLogService 方法名 `query/findById`。切点只匹配 create/update/delete → 审计读不触发审计。

### 11. 前端 AuditLogsPage —— 只读 + 新「系统」顶级 Sider 组

**方案**：
- 新路由 `/sys/audit-logs` → `AuditLogsPage`（复用 Table + Pagination；过滤栏 actor/entityType/entityId/action；**无新建/编辑/删除按钮、无 EditDrawer**）。
- Sider 新增**第 5 个顶级组「系统」**（key `sys`，置于「人事配置」之后），含 1 项「审计日志」。
- `api/auditLog.ts`：`listAuditLogs(params)` + `AuditLog` 类型。

**为什么**：审计是系统/管理员关注点，独立于「人事配置」；新「系统」组语义清晰，为后续系统级页面（AI 错误公示板等）预留位置。

**影响**：Sider 顶级组 4 → 5；`AppLayout.test` 既有「4 顶级组」断言（TC-FES-PROD-001）需同步改 5。

### 12. pom 依赖 + 配置位置

**方案**：`backend/pom.xml` `</dependencies>`（第 82 行）前加 `spring-boot-starter-aop`。`@EnableTransactionManagement` 配置类放 `com.rainier.config`（与现有 config 同包）或 `com.rainier.auditlog.config`。

---

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│ 任意业务写: POST/PUT/DELETE /api/X                            │
│   → XxxController → XxxService.create/update/delete (@Transactional) │
│        │                                                      │
│        │  [tx advice — 最外层 (Decision 6)]                   │
│        │    [AuditAspect @AfterReturning — 内层 (Decision 2)] │
│        │       成功返回 → 解析 actor/entityType/action/id     │
│        │       → AuditLogRepository.save() 加入业务 tx        │
│        ▼                                                      │
│   业务提交 → 审计行一并提交；业务回滚 → 审计行一并回滚       │
└──────────────────────────────────────────────────────────────┘

com.rainier.auditlog (NEW)
  domain/AuditLog (extends BaseEntity; actor/entityType/entityId/action/summary)
  domain/AuditAction (常量 CREATE/UPDATE/DELETE)
  repository/AuditLogRepository (JpaSpecificationExecutor)
  service/AuditLogService (record(...) 写 + query/findById 读 — 无 create/update/delete 命名)
  controller/AuditLogController (GET only, /api/audit-logs)
  aspect/AuditAspect (@Aspect, 3 pointcut + @AfterReturning)
com.rainier.config (or auditlog.config)
  AuditTxConfig (@EnableTransactionManagement(order = HIGHEST_PRECEDENCE))

frontend
  api/auditLog.ts
  pages/AuditLog/{AuditLogsPage.tsx, index.tsx}
  AppRoutes: + /sys/audit-logs
  AppLayout: + 「系统」顶级组 (第 5 组) → 审计日志
```

### 数据流：创建一条需求 → 自动审计

```
POST /api/requirements {...}
  RequirementController.create → RequirementService.create(req) [tx 开始]
    业务逻辑 → saveAndFlush → return RequirementDetail(id=5)
    [AuditAspect @AfterReturning]
      actor = AuditorAware.getCurrentAuditor()  // "alice"
      entityType = "REQUIREMENT" (RequirementService → ...)
      action = "CREATE"
      entityId = ret.getId() = 5
      summary = "CREATE REQUIREMENT#5"
      auditLogRepository.save(AuditLog{...})  // 加入当前 tx
  [tx 提交] → 业务行 + 审计行一起落库
→ 201 RequirementDetail
后续 GET /api/audit-logs?entityType=REQUIREMENT&entityId=5 → 见该行
```

---

## Risks / Trade-offs

| 风险 | 缓解 |
|------|------|
| 切面跑在事务外 → 审计落独立 tx，失去回滚原子性 | Decision 6：`@EnableTransactionManagement(order=HIGHEST_PRECEDENCE)` 让 tx 最外层、审计内层；rollback 测试用「业务方法抛异常 → 审计行不存在」验证（@AfterReturning 不触发 + 同事务双保险）。 |
| CGLIB 代理导致类名变 `Xxx$$EnhancerBySpringCGLIB` | Decision 3 用 `getSignature().getDeclaringType().getSimpleName()` 拿声明类，非代理类。 |
| 反射 getId() 在某 Detail 无 getId 时抛异常阻断业务 | Decision 5 兜底：反射异常 → entityId=null + WARN 日志，不抛、不阻断业务；所有现有 *Detail 均有 getId（已核）。 |
| 自审计递归（审计写触发审计写） | Decision 2/10：AuditLogService 无 create/update/delete 命名方法；切面走 repo.save 直连 → 切点不匹配。加测试断言「查 audit_log 不产生 audit_log」。 |
| 切面匹配到非预期的 *Service 方法（如未来新增 create 重载） | 切点限定 create/update/delete 三名 + 测试覆盖全 17 service 各产生 1 行；新增 service 自动纳入（预期行为）。 |
| @EnableTransactionManagement(order) 改全局 tx 顺序影响现有行为 | 当前无其它自定义 @Aspect；提升 tx order 仅改变「tx 相对切面的内外」，不改 tx 语义本身；全量 293 回归测试守护。 |
| audit_log 写入给每次业务写 +1 INSERT，高频写性能 | v0.0.15 同步写可接受（单 INSERT）；异步写已列入显式排除，留后续。perf 测试断言写路径 SQL 增量有界。 |
| Sider 4→5 顶级组破坏既有「4 组」测试 | Decision 11：同步改 AppLayout.test TC-FES-PROD-001 为 5 组；grep 校对其它组断言不动。 |
| 前端审计页无权限收口，任何登录用户可见全审计 | 已列显式排除（权限后续）；v0.0.15 先交付查询能力。 |
