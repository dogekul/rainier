# v1 id 迁移 测试方案

> 版本：v1.0  日期：2026-06-05
> 对应 Phase 2 spec：5 个 MODIFIED capability spec

## 一、测试策略

迁移类变更的特殊性：**v1 已有 59 + 11 个测试套件作为行为锚点**。本变更的核心断言是"迁移后这些测试全部仍绿"。所以测试增量小：

- **回归（V1-Preserved）**：v1 的 59 后端 + 11 前端测试经字面修改（断言中 UUID → 数字）后必须 100% 通过
- **新增（MIG）**：5 条专项测试验证类型契约本身

### 1.1 测试金字塔

- 单元（10%）：`BaseEntity` 反射 / TreeSelect 类型契约
- 集成（85%）：v1 MockMvc / RTL 全套，断言改字面
- E2E（5%）：docker compose 起栈 + SHOW COLUMNS + curl POST 看返回值类型

### 1.2 测试原则

- 行为锚点优先：每个 v1 已有 TC 必须仍绿
- 类型契约显式断言：5 条 TC-MIG 直接验"id 是数字"
- 数据策略 wipe：每次 docker compose 起栈必从空 schema 开始
- 不写 SQL 迁移脚本（dev wipe）

### 1.3 已有测试资产

59 backend (v1) + 11 frontend (v1) = 70 测试，全部需保持绿。

## 二、详细测试案例

### 回归保留（V1-Preserved，70 条，复用 v1 TC-ID）

| 范围 | TC 数 | 改造点 |
|---|---|---|
| TC-ORG-001..019 | 19 | matchesPattern + readId 类型 + ghost id 占位 |
| TC-USR-001..013 | 13 | 同上 |
| TC-UOR-001..012 | 12 | 同上 + ghost id 占位 `999_999L` |
| TC-PAG-001..003 | 3 | 不动（id 类型与 PageParams 无关）|
| TC-BES-201/202/203 | 3 | 软删除断言中 id 类型 + native query 参数 |
| TC-FES-201/202/203 | 3 | TreeNode.id 用 number 1,2,3 |
| TC-DRT-201 | 1 | DESCRIBE 断言列类型为 BIGINT |
| TC-TRT-201 | 1 | 不动 |
| v0 测试 (TC-HLT-001 / TC-AUT-001..004 / TC-FES-001..004 等) | 15 | 不动（不涉及业务 id） |
| **合计** | **70** | |

### 新增（MIG）

#### TC-MIG-001 — BaseEntity 字段反射

| 字段 | 内容 |
|---|---|
| ID | TC-MIG-001 |
| 对应 Spec | `backend-scaffold/spec.md` → BaseEntity 字段反射断言 |
| 优先级 | P0 |
| 预置 | Spring Boot test 上下文加载 |
| 输入 | `BaseEntity.class.getDeclaredField("id")` |
| 预期 | `Field.getType() == Long.class`；含 `@GeneratedValue`；strategy = IDENTITY |
| 当前状态 | ❌ 新增 |

#### TC-MIG-002 — @PathVariable 非数字 → 400 JSON

| 字段 | 内容 |
|---|---|
| ID | TC-MIG-002 |
| 对应 Spec | `backend-scaffold/spec.md` → 非数字 id 路径 |
| 优先级 | P0 |
| 预置 | backend 运行 |
| 输入 | `GET /api/organizations/not-a-number` |
| 预期 | 400 / `application/json` / `body.message` 非空 / 无 stack trace |
| 当前状态 | ❌ 新增 |

#### TC-MIG-003 — Organization 三层级 path 为 /digits/digits/digits

| 字段 | 内容 |
|---|---|
| ID | TC-MIG-003 |
| 对应 Spec | `entity-organization/spec.md` → 三层级路径串 |
| 优先级 | P0 |
| 预置 | DB 空 |
| 输入 | 顺次 POST 3 个组织 A→B→C |
| 预期 | C.path 完全等于 `/1/2/3`；正则 `^/(\d+)(/\d+)*$` 命中 |
| 当前状态 | ❌ 新增 |

#### TC-MIG-004 — E2E DESCRIBE 三表显示 id BIGINT

| 字段 | 内容 |
|---|---|
| ID | TC-MIG-004 |
| 对应 Spec | `entity-organization` / `entity-user` / `entity-user-organization` 综合 |
| 优先级 | P0 |
| 预置 | docker compose down -v + up -d --build 后等 healthy |
| 输入 | `docker exec rainier-mysql mysql ... -e "DESCRIBE rainier_organization;"` 等 3 表 |
| 预期 | 每表 id 列类型显示 `BIGINT`；含 `auto_increment` 标记；不出现 `varchar(32)` |
| 当前状态 | ❌ 新增（半自动 Bash） |

#### TC-MIG-005 — E2E POST 返回 body.id 为 JSON 数字

| 字段 | 内容 |
|---|---|
| ID | TC-MIG-005 |
| 对应 Spec | `entity-organization` → 根节点 id 为数字 |
| 优先级 | P0 |
| 预置 | docker stack healthy |
| 输入 | `curl -X POST http://localhost/api/organizations -d '{"type":"COMPANY","code":"X","name":"X"}'` |
| 预期 | 201；`body \| jq '.id \| type'` 输出 `"number"`；body.id 是正整数 |
| 当前状态 | ❌ 新增（半自动 Bash） |

## 三、测试执行矩阵

| 模块 | 单元 | 集成 | E2E |
|---|---|---|---|
| BaseEntity 反射 | TC-MIG-001 | — | — |
| @PathVariable 校验 | — | TC-MIG-002 | — |
| Organization | — | 19 v1 改 + TC-MIG-003 | TC-MIG-004/005 |
| User | — | 13 v1 改 | — |
| UserOrganization | — | 12 v1 改 | — |
| 前端类型契约 | tsc -b | RTL 11 v1 改 | 浏览器烟测 |
| Docker schema | — | — | TC-MIG-004 |

## 四、回归风险矩阵

| 风险区域 | 改动 | 已有保护 | 等级 |
|---|---|---|---|
| BaseEntity id 类型 | 全栈 | 反射 + 编译期 | 🟢 低 |
| Hibernate IDENTITY 在空 schema 重生 | wipe 后从 0 开始 | E2E + 健康检查 | 🟢 低 |
| `path` 字段 LIKE 查询语义 | 内容变短 | TC-ORG-011/012 级联回归 | 🟡 中 |
| 前端 TS 编译广播 | tsc 全编译 | `npm run build` | 🟢 低 |
| 测试断言遗漏 | `git grep "[0-9a-f]{32}"` 应为 0 | Phase 5 SC | 🟡 中 |
| `@PathVariable Long` 非数字异常默认 500 | 需显式 handler 才返 400 | 决策 §6 新增 handler | 🟡 中 |
| v0 entity / auth flow | 完全不动 | v0 测试不改 | 🟢 低 |
| demand-requirement 文档悬挂 | paused 状态保留 | 由那个变更解锁时维护 | 🟢 低 |

## 五、建议补充顺序

1. **P0 必补**：TC-MIG-001..005 + 70 v1 回归（含 TC-ORG-011/012 树缓存级联回归）
2. **P1 提交前**：所有 v1 frontend 测试改 number fixtures
3. **P2 可灰度**：v0 测试 / auth flow 不动
