# 组织维度骨架 测试方案

> 版本：v1.0  日期：2026-06-04
> 对应 spec：8 个 capability spec

## 一、测试策略

### 1.1 测试金字塔

- **单元（55%）**：service 业务逻辑（树缓存维护、is_primary 唯一性、软删保护）；前端 RTL 组件 + hook 单测
- **集成（40%）**：@SpringBootTest + @AutoConfigureMockMvc（每个 controller × CRUD）；前端 RTL 页 + MSW
- **E2E（5%）**：docker compose 起整栈，curl 创建/列表/删除/移动节点

### 1.2 测试原则

- 测试行为而非实现
- 后端单测用 H2，不依赖 docker
- 软删除测试同时覆盖"接口返回 404 / DB 仍存在"双向
- 树缓存测试同时断言 path + whole_name

### 1.3 已有测试资产

| 测试文件 | 用例数 | 类型 | 覆盖 |
|---|---|---|---|
| backend/src/test/.../*.java (v0) | 13 | 集成 | v0 health + auth |
| frontend/src/...*.test.tsx (v0) | 7 | 单元 | v0 ProtectedRoute / Login / tokens |

## 二、详细测试案例

### 功能 1：entity-organization

| TC | Scenario | P | Layer | 关键断言 |
|---|---|---|---|---|
| TC-ORG-001 | POST 创建根节点 | P0 | MockMvc | 201 / Location / path=/{id} / wholeName="总公司" |
| TC-ORG-002 | POST 创建子节点 + 路径派生 | P0 | MockMvc | path / whole_name 拼接正确 |
| TC-ORG-003 | POST 缺 name → 400 | P0 | MockMvc | fieldErrors[name] |
| TC-ORG-004 | POST (parent_id, code) 冲突 → 409 | P0 | MockMvc | 409 |
| TC-ORG-005 | POST parent_id 不存在 → 400 | P0 | MockMvc | message 含 not found |
| TC-ORG-006 | GET 存在 → 200 | P0 | MockMvc | 字段完整 |
| TC-ORG-007 | GET 已软删 → 404 | P0 | MockMvc | 404 |
| TC-ORG-008 | GET tree 排除软删 + 按 path 排序 | P0 | MockMvc | 返回 N-1 项 / path 字典序 |
| TC-ORG-009 | GET list type=DEPARTMENT 过滤 | P0 | MockMvc | total / 全 type=DEPARTMENT |
| TC-ORG-010 | GET list search 匹配 whole_name | P1 | MockMvc | 单条命中 |
| TC-ORG-011 | PUT name → 子孙 whole_name 级联 | P0 | MockMvc | A/B/C 三层级联正确 |
| TC-ORG-012 | PUT /parent 移动 → path + whole_name 级联 | P0 | MockMvc | 自身 + 子孙重算 |
| TC-ORG-013 | PUT /parent 移到子孙 → 409 防环 | P0 | MockMvc | 409 |
| TC-ORG-014 | DELETE 无子无关联 → 204 + DB del_flag=1 | P0 | MockMvc | 204 / DB 行仍在 / del_flag=1 |
| TC-ORG-015 | DELETE 有子节点 → 409 | P0 | MockMvc | 409 |
| TC-ORG-016 | DELETE 有 user_organization → 409 | P0 | MockMvc | 409 |
| TC-ORG-017 | 前端 /org/organizations 列表 + Tree toggle | P0 | RTL+MSW | 行渲染 / tree 展开 |
| TC-ORG-018 | 前端 TreeSelect 选父节点 | P1 | RTL+MSW | 树面板 / 选择 / 填表 |
| TC-ORG-019 | 前端删除二次确认 | P1 | RTL | ConfirmDialog / 取消/确认 |

### 功能 2：entity-user

| TC | Scenario | P | Layer | 关键断言 |
|---|---|---|---|---|
| TC-USR-001 | POST 最小 payload → 201 默认值 | P0 | MockMvc | id UUID / isInternal=true / enabled=true / delFlag=false |
| TC-USR-002 | POST login_name 冲突 → 409 | P0 | MockMvc | 409 |
| TC-USR-003 | POST code 冲突 → 409 | P0 | MockMvc | 409 |
| TC-USR-004 | POST email 格式非法 → 400 | P0 | MockMvc | fieldErrors[emailAddress] |
| TC-USR-005 | GET 存在 → 200 | P0 | MockMvc | 字段完整 |
| TC-USR-006 | GET 已软删 → 404 | P0 | MockMvc | 404 |
| TC-USR-007 | GET list search 跨字段 | P0 | MockMvc | login_name/name/code/email 任一匹配 |
| TC-USR-008 | GET list isInternal=false 过滤 | P1 | MockMvc | 全 isInternal=false |
| TC-USR-009 | PUT 修改 name + enabled，login_name 不变 | P0 | MockMvc | login_name 不变 |
| TC-USR-010 | DELETE 无组织归属 → 204 | P0 | MockMvc | 204 / DB del_flag=1 |
| TC-USR-011 | DELETE 有在岗归属 → 409 | P0 | MockMvc | 409 |
| TC-USR-012 | 前端 /org/users 列表 + 搜索 | P0 | RTL+MSW | 行渲染 / 搜索筛选 |
| TC-USR-013 | 前端编辑无密码字段 | P1 | RTL | 表单无 password input |

### 功能 3：entity-user-organization

| TC | Scenario | P | Layer | 关键断言 |
|---|---|---|---|---|
| TC-UOR-001 | POST 合法归属 → 201 | P0 | MockMvc | 201 / leftAt=null |
| TC-UOR-002 | POST (user_id, org_id) 重复 → 409 | P0 | MockMvc | 409 |
| TC-UOR-003 | POST user 不存在 → 400 | P0 | MockMvc | message 含 not found |
| TC-UOR-004 | POST 设 is_primary=true → 旧 primary demote | P0 | MockMvc | 旧行 isPrimary=false / 新行 isPrimary=true / DB only 1 primary |
| TC-UOR-005 | GET list 按 userId 过滤 | P0 | MockMvc | total 正确 |
| TC-UOR-006 | GET list 按 organizationId 过滤 | P0 | MockMvc | total 正确 |
| TC-UOR-007 | GET list 含 enrichment（user.name, org.name） | P0 | MockMvc | enrichment 字段非空 |
| TC-UOR-008 | PUT 设 left_at → 表示离职 | P0 | MockMvc | leftAt 填入 |
| TC-UOR-009 | PUT role MEMBER → HEAD | P0 | MockMvc | role 更新 |
| TC-UOR-010 | DELETE 硬删除 | P0 | MockMvc | 204 / DB 行物理消失 |
| TC-UOR-011 | 前端 /org/user-organizations 列表 + 新建 | P0 | RTL+MSW | 表单 / 提交 |
| TC-UOR-012 | 前端编辑设 is_primary 调旧值 demote 提示 | P1 | RTL+MSW | UI 提示 |

### 功能 4：pagination-envelope

| TC | Scenario | P | Layer | 关键断言 |
|---|---|---|---|---|
| TC-PAG-001 | envelope 字段稳定 | P0 | MockMvc | 仅 4 字段 content/page/size/total |
| TC-PAG-002 | size=101 → 400 | P0 | MockMvc | 400 / message |
| TC-PAG-003 | 默认 page=0 size=20 | P0 | MockMvc | 默认值 |

### 功能 5：backend-scaffold MOD

| TC | Scenario | P | Layer | 关键断言 |
|---|---|---|---|---|
| TC-BES-201 | Schema 自动生成（dev profile 启动后 MySQL 含 3 张表）| P0 | E2E + Bash | docker exec mysql SHOW TABLES → rainier_organization / rainier_user / rainier_user_organization。**注**：Flyway 因 8.5.13 community 不支持 MySQL 8 已禁用（pending-adjustments #1），改由 Hibernate ddl-auto=update 生成 schema；无 flyway_schema_history 表 |
| TC-BES-202 | Bean Validation 错误 → fieldErrors 数组 | P0 | MockMvc | message="Validation failed" / fieldErrors[].field |
| TC-BES-203 | SoftDelete: DELETE → UPDATE，findById empty | P0 | JPA 单测 | DB 行存在但 del_flag=1，findById Optional.empty |

### 功能 6：frontend-scaffold MOD

| TC | Scenario | P | Layer | 关键断言 |
|---|---|---|---|---|
| TC-FES-201 | Sider 含组织菜单组 + 3 项 | P0 | RTL | 菜单 / 跳转 |
| TC-FES-202 | Table 渲染 columns + rows | P0 | RTL | header / body row 数 |
| TC-FES-203 | TreeSelect 选父节点 | P1 | RTL+MSW | 树面板 / 选择回填 |

### 功能 7：dev-runtime MOD

| TC | Scenario | P | Layer | 关键断言 |
|---|---|---|---|---|
| TC-DRT-201 | Compose up 后 MySQL 含 3 业务表 + Hibernate 自动生成 schema | P0 | Bash + curl | `SHOW TABLES` 含 rainier_organization / rainier_user / rainier_user_organization（**3 表**，无 flyway_schema_history）/ curl /api/health 200 |

### 功能 8：test-runtime MOD

| TC | Scenario | P | Layer | 关键断言 |
|---|---|---|---|---|
| TC-TRT-201 | mvn test 无 docker 通过 + 用 H2 + 无 Flyway | P0 | Bash | mvn test exit 0 / log 含 H2 / log 不含 flyway migration |

## 三、测试执行矩阵

| 模块 | 单元 | 集成 | E2E |
|---|---|---|---|
| entity-organization | service 树维护单测 | MockMvc × 16 | TC-DRT-201 |
| entity-user | service 单测 | MockMvc × 11 | (同) |
| entity-user-organization | service is_primary 单测 | MockMvc × 10 | (同) |
| pagination-envelope | — | MockMvc × 3 | — |
| backend-scaffold MOD | JPA 软删单测 | MockMvc × 2 | TC-BES-201 |
| frontend-scaffold MOD | RTL × 3 | — | — |
| dev-runtime MOD | — | — | TC-DRT-201 |
| test-runtime MOD | — | — | TC-TRT-201 |

## 四、回归风险矩阵

| 风险区域 | 改动 | 已有回归保护 | 风险等级 |
|---|---|---|---|
| Flyway 迁移路径 | 全新 V1 (3 表 + FK + 索引) | E2E + 启动日志 + flyway_schema_history 校验 | 🟡 中 |
| H2 ↔ MySQL DDL 漂移 | test 用 H2 + ddl-auto | E2E 真实 MySQL + Flyway | 🟡 中 |
| 软删除全局模式 | 全新 @SQLDelete + @Where | JPA 单测 + 接口正反路径 | 🟡 中（漏掉 @Where 会泄露软删数据） |
| 树缓存维护（path / whole_name） | 全新 | service 单测 + MockMvc 级联场景 | 🟡 中（深度递归边界） |
| is_primary 单一性 | 全新 | service 单测 + MockMvc | 🟢 低 |
| UUID 主键 | 全新 | JPA 生成器单测 | 🟢 低 |
| 通用前端组件 (Table/Drawer/TreeSelect) | 全新 | RTL 单测 + 3 页消费 | 🟢 低 |
| PageResponse 反序列化 | 全新 | 前后端共契约 TS + MockMvc 校验 | 🟢 低 |
| FK 删除保护（服务层） | 全新 | MockMvc 正反路径 | 🟢 低 |
| Bean Validation 错误形态 | 修改 GlobalExceptionHandler | 已有 v0 异常测 + 新 fieldErrors 测 | 🟢 低 |
| Sider 导航对未登录路由 | 修改 AppLayout | ProtectedRoute 测试不变 | 🟢 低 |

## 五、建议补充顺序

1. **第一优先 P0（v0 上线必补 = 41 条）**：
   TC-ORG-001..009/011..017, TC-USR-001..007/009..012, TC-UOR-001..011, TC-PAG-001..003, TC-BES-201..203, TC-FES-201/202, TC-DRT-201, TC-TRT-201

2. **第二优先 P1（提交前补 = 7 条）**：
   TC-ORG-010/018/019, TC-USR-008/013, TC-UOR-012, TC-FES-203
