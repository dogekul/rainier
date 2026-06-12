# v0.0.17-milestone 测试方案与详细案例

> 版本：v0.0.17-milestone
> 创建日期：2026-06-12
> 对应 Phase 2 Spec：entity-milestone (NEW) + entity-project (MOD) + frontend-scaffold (MOD)

## 一、测试策略

### 1.1 测试金字塔

- **后端集成（主力）**：`@SpringBootTest`+`MockMvc` 覆盖 Milestone create/list/update/delete + 校验 + 级联软删。
- **组件（前端）**：vitest + RTL 覆盖 ProjectsPage 里程碑按钮 + MilestonesPanel CRUD。
- **E2E**：docker compose + curl + MySQL，验证 +1 表(19)、级联软删、既有数据不变。

### 1.2 测试原则

- 纯新增实体 + 一处既有 delete 扩展（级联）;既有 Project delete 的 FK 409 行为保持绿。
- standing：测试不删改存量业务数据;级联只作用于被删测试项目自身里程碑;E2E 用新建项目验证级联。
- 复合唯一 (projectId,code)：必须既测「同项目重复 409」又测「跨项目同 code 放行」。

### 1.3 已有测试资产

| 测试文件 | 类型 | 复用点 |
|----------|------|--------|
| ProjectControllerCreateTest / SprintController*（既有） | 集成 | MockMvc + ObjectNode seed 范本 |
| DanglingProjectIdCleanupTest（既有） | 集成 | 非事务 saveAndFlush+run+findById 范本 |
| SprintFeaturePanel.test.tsx（既有） | 组件 | 内联面板测试范本 |

## 二、详细测试案例

### 功能 1：创建里程碑（entity-milestone）

#### 案例 1.1 — 默认 PLANNED / sortOrder 0
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-001 |
| **对应 Spec** | entity-milestone → 最小 payload 创建 + 默认 PLANNED / sortOrder 0 |
| **优先级** | P0 |
| **预置条件** | 项目 id=1 存在 |
| **输入** | `POST /api/milestones` `{projectId:1,code:"M-1",name:"评审",targetDate:"2026-07-01"}` |
| **预期结果** | 201;status="PLANNED";sortOrder=0;projectId=1;targetDate="2026-07-01" |
| **当前状态** | ❌ 测试缺 |

#### 案例 1.2 — 显式 status/sortOrder/actualDate
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-002 |
| **对应 Spec** | entity-milestone → 显式 status / sortOrder / actualDate 创建 |
| **优先级** | P0 |
| **预置条件** | 项目 id=1 存在 |
| **输入** | `POST` body 含 status:"REACHED",sortOrder:5,actualDate:"2026-06-30" |
| **预期结果** | 201;status="REACHED";sortOrder=5;actualDate="2026-06-30" |
| **当前状态** | ❌ 测试缺 |

### 功能 2：创建校验（entity-milestone）

#### 案例 2.1 — projectId 不存在 → 400
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-003 |
| **对应 Spec** | entity-milestone → projectId 不存在被拒 |
| **优先级** | P0 |
| **输入** | `POST` body 含 projectId:999999 |
| **预期结果** | 400;message 含 "project not found" |
| **当前状态** | ❌ 测试缺 |

#### 案例 2.2 — 缺 targetDate → 400
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-004 |
| **对应 Spec** | entity-milestone → 缺 targetDate 被拒 |
| **优先级** | P0 |
| **输入** | `POST` `{projectId:1,code:"M-2",name:"X"}`（无 targetDate） |
| **预期结果** | 400;fieldErrors[*].field 含 "targetDate" |
| **当前状态** | ❌ 测试缺 |

#### 案例 2.2b — 缺 code → 400
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-014 |
| **对应 Spec** | entity-milestone → 缺 code 被拒 |
| **优先级** | P0 |
| **输入** | `POST` `{projectId:1,name:"X",targetDate:"2026-07-01"}`（无 code） |
| **预期结果** | 400;fieldErrors[*].field 含 "code" |
| **当前状态** | ❌ 测试缺 |

#### 案例 2.3 — 非法 status → 400
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-005 |
| **对应 Spec** | entity-milestone → 非法 status 被拒 |
| **优先级** | P0 |
| **输入** | `POST` body 含 status:"DONE" |
| **预期结果** | 400;message 含 "invalid status" |
| **当前状态** | ❌ 测试缺 |

#### 案例 2.4 — 同项目 code 重复 → 409
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-006 |
| **对应 Spec** | entity-milestone → 同项目内 code 重复被拒 |
| **优先级** | P0 |
| **预置条件** | 项目 id=1 已有里程碑 code="M-1" |
| **输入** | `POST` body 含 projectId:1,code:"M-1" |
| **预期结果** | 409;message 含 "code already exists" |
| **当前状态** | ❌ 测试缺 |

#### 案例 2.5 — 跨项目同 code → 201
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-007 |
| **对应 Spec** | entity-milestone → 不同项目可用相同 code |
| **优先级** | P0 |
| **预置条件** | 项目 id=1 有 code="M-1"，项目 id=2 存在 |
| **输入** | `POST` body 含 projectId:2,code:"M-1" |
| **预期结果** | 201 |
| **当前状态** | ❌ 测试缺 |

### 功能 3：查询（过滤 + 排序，entity-milestone）

#### 案例 3.1 — projectId 过滤 + sortOrder 升序
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-008 |
| **对应 Spec** | entity-milestone → 按 projectId 过滤并按 sortOrder 升序 |
| **优先级** | P0 |
| **预置条件** | 项目1 有 A(sortOrder=2)+B(sortOrder=1)，项目2 有 C |
| **输入** | `GET /api/milestones?projectId=1` |
| **预期结果** | total=2;全 projectId=1;content[0].sortOrder ≤ content[1].sortOrder（B 先） |
| **当前状态** | ❌ 测试缺 |

#### 案例 3.2 — status 过滤
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-009 |
| **对应 Spec** | entity-milestone → 按 status 过滤 |
| **优先级** | P0 |
| **预置条件** | 项目1 有 2 PLANNED + 1 REACHED |
| **输入** | `GET /api/milestones?projectId=1&status=PLANNED` |
| **预期结果** | total=2;全 status="PLANNED" |
| **当前状态** | ❌ 测试缺 |

### 功能 4：更新（entity-milestone）

#### 案例 4.1 — 标记达成 PLANNED→REACHED + actualDate
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-010 |
| **对应 Spec** | entity-milestone → 标记达成 |
| **优先级** | P0 |
| **预置条件** | 里程碑 id=X status="PLANNED" |
| **输入** | `PUT /api/milestones/X` 含 status:"REACHED",actualDate:"2026-07-02"（+code/name/targetDate） |
| **预期结果** | 200;status="REACHED";actualDate="2026-07-02" |
| **当前状态** | ❌ 测试缺 |

#### 案例 4.2 — 调整 sortOrder + targetDate
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-011 |
| **对应 Spec** | entity-milestone → 调整 sortOrder 与 targetDate |
| **优先级** | P0 |
| **输入** | `PUT` 含 sortOrder:9,targetDate:"2026-08-01" |
| **预期结果** | 200;sortOrder=9;targetDate="2026-08-01" |
| **当前状态** | ❌ 测试缺 |

#### 案例 4.3 — 更新非法 status → 400
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-012 |
| **对应 Spec** | entity-milestone → 更新非法 status 被拒 |
| **优先级** | P0 |
| **输入** | `PUT` 含 status:"XXX" |
| **预期结果** | 400;message 含 "invalid status" |
| **当前状态** | ❌ 测试缺 |

### 功能 5：软删（entity-milestone）

#### 案例 5.1 — 软删 → 204 + 404
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-013 |
| **对应 Spec** | entity-milestone → 软删成功 |
| **优先级** | P0 |
| **输入** | `DELETE /api/milestones/X` |
| **预期结果** | 204;后续 `GET /api/milestones/X` → 404 |
| **当前状态** | ❌ 测试缺 |

### 功能 6：删除项目级联软删里程碑（entity-project MOD）

#### 案例 6.1 — 级联软删
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-CAS-001 |
| **对应 Spec** | entity-project → 删除有里程碑且无其它引用的项目级联软删里程碑 |
| **优先级** | P0 |
| **预置条件** | 项目 id=P 有 2 里程碑，无 Requirement/UserRole/Task 引用 |
| **输入** | `DELETE /api/projects/P` |
| **预期结果** | 204;两里程碑 del_flag=1;`GET /api/milestones?projectId=P` total=0 |
| **当前状态** | ❌ 测试缺 |

#### 案例 6.2 — 被 Requirement 引用仍 409 且里程碑不删
| 字段 | 内容 |
|------|------|
| **ID** | TC-MILE-CAS-002 |
| **对应 Spec** | entity-project → 被 Requirement 引用的项目仍 409 且里程碑不被删 |
| **优先级** | P0 |
| **预置条件** | 项目 id=P 有 1 里程碑 + ≥1 Requirement(projectId=P) |
| **输入** | `DELETE /api/projects/P` |
| **预期结果** | 409;message 含 "project has linked requirements";里程碑仍 del_flag=0（回滚） |
| **当前状态** | ❌ 测试缺 |

### 功能 7：前端里程碑面板（frontend-scaffold MOD）

#### 案例 7.1 — 里程碑按钮展开面板
| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-MILE-001 |
| **对应 Spec** | frontend-scaffold → 点击里程碑按钮展开内联面板 |
| **优先级** | P0 |
| **输入** | ProjectsPage 渲染含 id=7 行，点「里程碑」按钮 |
| **预期结果** | 渲染 `milestones-panel-7`;listMilestones 调用含 projectId:7 |
| **当前状态** | ❌ 测试缺 |

#### 案例 7.2 — 面板列出里程碑
| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-MILE-002 |
| **对应 Spec** | frontend-scaffold → 面板列出该项目里程碑 |
| **优先级** | P0 |
| **预置条件** | listMilestones({projectId:7}) mock 返回 2 里程碑 |
| **预期结果** | 面板显示 2 里程碑的 name 与 status |
| **当前状态** | ❌ 测试缺 |

#### 案例 7.3 — 面板新建携带 projectId
| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-MILE-003 |
| **对应 Spec** | frontend-scaffold → 面板新建里程碑携带 projectId |
| **优先级** | P0 |
| **输入** | 面板填 name/targetDate 点新建 |
| **预期结果** | createMilestone 调用 body 含 projectId:7 |
| **当前状态** | ❌ 测试缺 |

#### 案例 7.4 — 面板删除里程碑
| 字段 | 内容 |
|------|------|
| **ID** | TC-FES-MILE-004 |
| **对应 Spec** | frontend-scaffold → 面板删除里程碑 |
| **优先级** | P0 |
| **输入** | 点里程碑 id=11 的删除并确认 |
| **预期结果** | deleteMilestone 调用参数为 11 |
| **当前状态** | ❌ 测试缺 |

### 功能 8：E2E

#### 案例 8.1 — 表19 + 级联 + 存量不变
| 字段 | 内容 |
|------|------|
| **ID** | TC-E2E-MILE-001 |
| **对应 Spec** | entity-milestone + entity-project + standing |
| **优先级** | P0 |
| **预置条件** | docker compose 重建 backend（MySQL 卷保留） |
| **输入** | 启动后 `SHOW TABLES`;新建测试项目+里程碑;查/改/删里程碑;删测试项目验级联;查存量 3 项目 |
| **预期结果** | SHOW TABLES=19(+rainier_milestone);CRUD 全通;删项目级联软删里程碑;存量 3 项目及其字段一字未改;清理测试数据 |
| **当前状态** | ❌ 测试缺 |

## 三、测试执行矩阵

| 功能模块 | 集成 | 组件 | E2E | 状态 |
|----------|------|------|-----|------|
| Milestone create + 校验 | TC-MILE-001..007 + TC-MILE-014 | — | TC-E2E-MILE-001 | 🔴→待补 |
| Milestone list 过滤排序 | TC-MILE-008/009 | — | — | 🔴→待补 |
| Milestone update + 软删 | TC-MILE-010..013 | — | TC-E2E-MILE-001 | 🔴→待补 |
| 项目删除级联软删里程碑 | TC-MILE-CAS-001/002 | — | TC-E2E-MILE-001 | 🔴→待补 |
| 前端 里程碑按钮 + 面板 CRUD | — | TC-FES-MILE-001..004 | — | 🔴→待补 |

## 四、回归风险矩阵

| 风险区域 | v0.0.17 改动 | 已有回归保护 | 风险等级 |
|----------|-------------|-------------|---------|
| 新 Milestone 实体/CRUD | 全新 | 本版新增测试 | 🟢低 |
| ProjectService.delete 级联 | 现有 FK 409 链后插入级联软删 | 既有 ProjectControllerDeleteTest（409 链）+ 新级联测试 | 🟡中（改既有 delete） |
| ProjectsPage 既有渲染/抽屉 | 加里程碑按钮 + 面板 | 既有 ProjectsPage.test | 🟢低 |
| 新表加载 | rainier_milestone | E2E 表数 + 存量不变断言 | 🟢低 |

## 五、建议补充顺序

1. **第一优先（部署前必补，全 P0）**：TC-MILE-001..013 + TC-MILE-CAS-001/002 + TC-FES-MILE-001..004 + TC-E2E-MILE-001
2. **第二优先（P1）**：无
3. **第三优先（P2）**：无
