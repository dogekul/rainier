# Test Plan: v0.0.49 — 项目编号自动生成

## 后端 TC
| TC | 场景 | 断言 |
|---|---|---|
| TC-CODE-01 | 创建 EXTERNAL_DELIVERY 无 code | 201，code 匹配 `^ED-\d+$` |
| TC-CODE-02 | 创建默认（CASUAL）无 code | 201，code 匹配 `^LT-\d+$` |
| TC-CODE-03 | 创建 CORE_FEATURE / CORE_TECH | code 前缀 CF / CT |
| TC-CODE-04 | 传 code 被忽略 | 传 `code:"X"` → 返回 code 仍 `{前缀}-{id}`（非 X） |
| TC-INI-03（改） | 立项内联新建仅名称 | 不传 projectCode → 200，新项目 code `^ED-\d+$`、type EXTERNAL_DELIVERY |
| 回归 | ProjectControllerCreateTest code 断言 | 改为匹配自动编号格式（原 PROJ-001 失效） |

## 前端 TC
| TC | 场景 | 断言 |
|---|---|---|
| TC-FES-PROJTYPE-004（改） | ProjectsPage 创建 | 无 projects-code 输入；createProject body 不含 code |
| 新增 | ProjectsPage 新建抽屉无编号 | queryByLabelText(/编号|编码/) 不存在 |
| TC-FDH-02（改） | 立项新建 | 无 delivery-new-code；initiate body = {projectName, projectOwnerUserId, decision} |

## E2E（docker 真栈）
- 创建 4 类型项目（无 code）→ code 前缀 LT/CF/CT/ED + id；传 code 被忽略。
- 立项内联新建（仅名称+负责人）→ 项目 code ED-{id}、商机关联。
- 既有项目 code 不变。throwaway 建即删。

## 回归风险
- 🟡 ProjectService.create 两步保存（占位→回填）；ProjectControllerCreateTest code 断言更新。
- 🟢 前端去输入纯减法；DTO code 改可空向后兼容。
