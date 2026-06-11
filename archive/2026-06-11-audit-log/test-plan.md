# v0.0.15-audit-log 测试方案与详细案例

> 版本：v0.0.15 | 创建：2026-06-11
> 对应 Phase 2 Spec：entity-audit-log(NEW) / frontend-scaffold(MOD)
> Baseline：v0.0.14-sprint-feature-link / 717ae99 / 293 backend + 54 frontend

## 一、测试策略

### 1.1 测试金字塔

- **集成层**（主战场）：MockMvc + H2，验证「业务写 → 自动审计行」的端到端切面行为（CREATE/UPDATE/DELETE 各产生 1 行 + actor + 失败不记 + 复合名 entityType + 防自递归）。
- **集成层（读）**：AuditLogController 查询过滤 + 分页倒序 + 单查 + append-only 无写端点。
- **单元层**：entityType 推导算法（camelCase→SCREAMING_SNAKE）纯函数（如抽成静态方法）。
- **性能层**：写路径审计 +1 INSERT 有界；审计 list batch。
- **前端**：Vitest + RTL，AuditLogsPage 渲染 + 过滤 + 只读 + Sider 第 5 组。
- **E2E**：docker compose + curl，18 表 + 建实体见审计行 + 现有数据未改。

### 1.2 测试原则

- 切面行为用「真实业务端点」驱动（POST/PUT/DELETE /api/X）而非直调切面，验证运行时织入生效（失败模式 f）。
- 失败不记：用既有冲突路径（重复 code 409）验证 @AfterReturning 只记成功。
- 同事务回滚：用「业务方法抛异常 → 审计行 + 业务行均不存在」验证。
- 防自递归：查 audit_log 前后行数不变。
- standing 约束：E2E 验证 17 张既有表数据未被审计逻辑改动。

### 1.3 已有资产复用

| 来源 | 复用点 |
|---|---|
| 各 entity ControllerCreate/Update/Delete 测试 | seeding 链 + 业务端点 |
| PageResponse / Specification list 模式 | audit-log 读 API |
| AppLayout.test (顶级组断言) | 改 4→5 组 |

## 二、详细测试案例

### 功能 1 — 切面自动审计（核心）

#### 案例 1.1 — 创建产生 CREATE 审计行
| 字段 | 内容 |
|---|---|
| **ID** | TC-AUD-001 |
| **对应 Spec** | entity-audit-log → 创建实体产生 CREATE 审计行 |
| **优先级** | P0 |
| **预置** | actor 注入 "alice"（mock request attr） |
| **输入** | `POST /api/requirements` 成功 → id=N |
| **预期** | audit_log +1；action=CREATE / entityType=REQUIREMENT / entityId=N / actor=alice |
| **状态** | ❌ |

#### 案例 1.2 — 更新产生 UPDATE 审计行
| 字段 | 内容 |
|---|---|
| **ID** | TC-AUD-002 |
| **对应 Spec** | → 更新实体产生 UPDATE 审计行 |
| **优先级** | P0 |
| **输入** | `PUT /api/requirements/N` 成功 |
| **预期** | audit_log +1；action=UPDATE / entityId=N |
| **状态** | ❌ |

#### 案例 1.3 — 删除产生 DELETE 审计行
| 字段 | 内容 |
|---|---|
| **ID** | TC-AUD-003 |
| **对应 Spec** | → 删除实体产生 DELETE 审计行 |
| **优先级** | P0 |
| **输入** | `DELETE /api/requirements/N` 成功 |
| **预期** | audit_log +1；action=DELETE / entityId=N |
| **状态** | ❌ |

#### 案例 1.4 — summary 文本格式
| 字段 | 内容 |
|---|---|
| **ID** | TC-AUD-004 |
| **对应 Spec** | → summary 文本格式 |
| **优先级** | P0 |
| **输入** | 创建 Requirement id=N |
| **预期** | summary == "CREATE REQUIREMENT#"+N |
| **状态** | ❌ |

#### 案例 1.5 — 失败不记审计
| 字段 | 内容 |
|---|---|
| **ID** | TC-AUD-005 |
| **对应 Spec** | 失败的写操作不记审计 → 业务校验失败不记审计 |
| **优先级** | P0 |
| **预置** | 已有 code=REQ-DUP |
| **输入** | 再 `POST /api/requirements` 同 code → 409 |
| **预期** | 409；audit_log 总数与请求前相同 |
| **状态** | ❌ |

#### 案例 1.6 — 同事务回滚无审计残留
| 字段 | 内容 |
|---|---|
| **ID** | TC-AUD-006 |
| **对应 Spec** | 审计写入与业务同事务 → 业务方法异常导致事务回滚时无审计残留 |
| **优先级** | P0 |
| **预置** | 一个会触发回滚的业务路径（如唯一约束/冲突在 flush 时抛，或测试桩 service 抛 RuntimeException） |
| **输入** | 触发该写操作 |
| **预期** | 业务行不存在 ∧ 对应审计行不存在 |
| **状态** | ❌ |

#### 案例 1.7 — 复合名 entityType 推导
| 字段 | 内容 |
|---|---|
| **ID** | TC-AUD-007 |
| **对应 Spec** | entityType 对复合类名正确推导 → 复合名 service 推导 |
| **优先级** | P0 |
| **输入** | `POST /api/sprint-features` 成功 |
| **预期** | 审计行 entityType=SPRINT_FEATURE_LINK / action=CREATE |
| **状态** | ❌ |

#### 案例 1.8 — 防自审计递归
| 字段 | 内容 |
|---|---|
| **ID** | TC-AUD-008 |
| **对应 Spec** | 审计读操作不被审计 → 查询审计日志不自增审计 |
| **优先级** | P0 |
| **预置** | audit_log 有 K 行 |
| **输入** | `GET /api/audit-logs` 一次 |
| **预期** | 200；audit_log 仍 K 行 |
| **状态** | ❌ |

#### 案例 1.9 — 多 service 各产生审计（抽样）
| 字段 | 内容 |
|---|---|
| **ID** | TC-AUD-009 |
| **对应 Spec** | 自动审计实体写操作（覆盖广度） |
| **优先级** | P0 |
| **输入** | 对 2 个不同实体（requirement / product）各成功 create 一次 |
| **预期** | 各恰 1 条审计行，entityType 各为 REQUIREMENT / PRODUCT（exactly-one-per-type 防误标/重复审计） |
| **状态** | ❌ |

### 功能 2 — 审计读 API

#### 案例 2.1 — 按 entityType+entityId 过滤
| 字段 | 内容 |
|---|---|
| **ID** | TC-AUD-010 |
| **对应 Spec** | 查询审计日志 → 按 entityType + entityId 过滤 |
| **优先级** | P0 |
| **预置** | Requirement id=5 有 2 审计行 + 其它实体若干 |
| **输入** | `GET /api/audit-logs?entityType=REQUIREMENT&entityId=5` |
| **预期** | total=2；每行 entityType=REQUIREMENT ∧ entityId=5 |
| **状态** | ❌ |

#### 案例 2.2 — 按 actor 过滤
| 字段 | 内容 |
|---|---|
| **ID** | TC-AUD-011 |
| **对应 Spec** | → 按 actor 过滤 |
| **优先级** | P0 |
| **预期** | actor=alice 的 total 正确，每行 actor=alice |
| **状态** | ❌ |

#### 案例 2.3 — 倒序（最新在前）
| 字段 | 内容 |
|---|---|
| **ID** | TC-AUD-012 |
| **对应 Spec** | → 倒序返回（最新在前） |
| **优先级** | P0 |
| **输入** | 先后产生 A、B；GET 列表 |
| **预期** | content[0] 为较新者 B |
| **状态** | ❌ |

#### 案例 2.4 — 单条查询字段集
| 字段 | 内容 |
|---|---|
| **ID** | TC-AUD-013 |
| **对应 Spec** | → 单条查询 |
| **优先级** | P0 |
| **输入** | `GET /api/audit-logs/{id}` |
| **预期** | 200；字段含 id/actor/entityType/entityId/action/summary/createTime |
| **状态** | ❌ |

#### 案例 2.5 — append-only 无写端点
| 字段 | 内容 |
|---|---|
| **ID** | TC-AUD-014 |
| **对应 Spec** | 审计日志 append-only → 无写端点 |
| **优先级** | P0 |
| **输入** | `POST /api/audit-logs` |
| **预期** | 404/405；源码 grep `@PostMapping`/`@PutMapping`/`@DeleteMapping` in AuditLogController = 0 |
| **状态** | ❌ |

### 功能 3 — 性能

#### 案例 3.1 — 写路径审计 SQL 增量有界
| 字段 | 内容 |
|---|---|
| **ID** | TC-PERF-AUD-001 |
| **对应 Spec** | design Risks（写性能） |
| **优先级** | P0 |
| **输入** | 一次 `POST /api/requirements`，Hibernate Statistics |
| **预期** | 审计带来的额外 INSERT ≤ 1（写路径总 SQL 在合理范围，范围断言防假绿） |
| **状态** | ❌ |

#### 案例 3.2 — audit-log list batch ≤ 阈值
| 字段 | 内容 |
|---|---|
| **ID** | TC-PERF-AUD-002 |
| **对应 Spec** | design Decision 9 |
| **优先级** | P0 |
| **预置** | 20 审计行 |
| **输入** | `GET /api/audit-logs?size=20` |
| **预期** | SQL 次数 ≥ 2 ∧ ≤ 3（page + count；audit 无富化 join） |
| **状态** | ❌ |

### 功能 4 — 前端

#### 案例 4.1 — Sider 含「系统」组 + 审计日志入口
| 字段 | 内容 |
|---|---|
| **ID** | TC-FES-AUD-001 |
| **对应 Spec** | frontend-scaffold → Sider 顶级菜单组「系统」 |
| **优先级** | P0 |
| **输入** | render AppLayout |
| **预期** | 5 顶级组末位「系统」；含「审计日志」→ /sys/audit-logs |
| **状态** | ❌ |

#### 案例 4.2 — AuditLogsPage 渲染表格 + 只读
| 字段 | 内容 |
|---|---|
| **ID** | TC-FES-AUD-002 |
| **对应 Spec** | AuditLogsPage 只读查询页 → 渲染审计表格 |
| **优先级** | P0 |
| **预置** | mock listAuditLogs 返回 2 行 |
| **输入** | render AuditLogsPage |
| **预期** | 表格含 2 行 + 表头(操作人/实体类型/实体ID/动作)；无「新建」按钮 |
| **状态** | ❌ |

#### 案例 4.3 — 过滤触发查询
| 字段 | 内容 |
|---|---|
| **ID** | TC-FES-AUD-003 |
| **对应 Spec** | → 按 entityType 过滤触发查询 |
| **优先级** | P0 |
| **输入** | 输入 entityType=REQUIREMENT + 触发 |
| **预期** | listAuditLogs 被调且 params.entityType=REQUIREMENT |
| **状态** | ❌ |

#### 案例 4.4 — /sys/audit-logs 路由
| 字段 | 内容 |
|---|---|
| **ID** | TC-FES-AUD-004 |
| **对应 Spec** | /sys/audit-logs 路由注册 → 路由直接访问 |
| **优先级** | P0 |
| **输入** | MemoryRouter /sys/audit-logs + AppRoutes |
| **预期** | 渲染 AuditLogsPage；grep AppRoutes ≥ 1 |
| **状态** | ❌ |

### 功能 5 — E2E

#### 案例 5.1 — 18 表 + 建实体见审计行
| 字段 | 内容 |
|---|---|
| **ID** | TC-E2E-AUD-001 |
| **对应 Spec** | proposal Success Criteria |
| **优先级** | P0 |
| **输入** | docker 重建 + SHOW TABLES + curl 建 requirement + 查 audit-logs |
| **预期** | 18 表含 rainier_audit_log；建后见 CREATE REQUIREMENT 审计行 |
| **状态** | ❌ |

#### 案例 5.2 — 现有数据未改
| 字段 | 内容 |
|---|---|
| **ID** | TC-E2E-AUD-002 |
| **对应 Spec** | standing 约束 |
| **优先级** | P0 |
| **输入** | 对比审计上线前后既有 17 表行数/内容 |
| **预期** | 既有表数据未被审计逻辑改动 |
| **状态** | ❌ |

## 三、测试执行矩阵

| 功能模块 | 单元 | 集成 | Perf | E2E | 状态 |
|---|---|---|---|---|---|
| 切面自动审计 | entityType 算法 | TC-AUD-001..009 (9) | TC-PERF-AUD-001 | TC-E2E-AUD-001 | 🟢 |
| 审计读 API | — | TC-AUD-010..014 (5) | TC-PERF-AUD-002 | — | 🟢 |
| 前端审计页 | — | TC-FES-AUD-001..004 (4) | — | manual | 🟢 |
| 数据安全 | — | — | — | TC-E2E-AUD-002 | 🟢 |

**TC 总数**：9 + 5 + 2 + 4 + 2 = **22 P0**

## 四、回归风险矩阵

| 风险区域 | v0.0.15 改动 | 已有回归保护 | 风险等级 |
|---|---|---|---|
| AOP 切面织入全 17 service 写方法（横切，改运行时行为） | 新 AuditAspect + tx order 提升 | 293 全量回归 + TC-AUD-009 多 service 抽样 | 🔴 |
| @EnableTransactionManagement(order) 改全局 tx 顺序 | 新配置类 | 293 全量回归（事务语义守护） + TC-AUD-006 回滚测试 | 🟡 |
| 反射 getId() / args[0] 解析在边界实体出错 | 切面解析逻辑 | TC-AUD-001/003/007（create 反射 + delete 首参 + 复合名） | 🟡 |
| 自审计递归 | 切点/服务命名约定 | TC-AUD-008 | 🟢 |
| Sider 4→5 顶级组破坏既有断言 | AppLayout + AppRoutes | TC-FES-AUD-001 改断言 + grep 校对 | 🟡 |
| 新表 ddl-auto=update | 自动建 | TC-E2E-AUD-001 SHOW TABLES | 🟢 |
| 写路径 +1 INSERT 性能 | 同步审计写 | TC-PERF-AUD-001 范围断言 | 🟢 |

**总评**：🔴 高: 1（AOP 横切织入）/ 🟡 中: 3 / 🟢 低: 3

## 五、建议补充顺序

1. **第一优先**（部署前必补）：全部 22 P0。
2. **第二优先**：无。
3. **第三优先**（后续版本）：
   - 字段级 diff（A2 推迟）
   - 审计读权限收口测试（仅管理员）
   - 异步写审计的并发/丢失测试
