# Test Report: v0.0.49 — 项目编号自动生成

> 基线 v0.0.48-project-types / c23f441。编号 = {类型前缀}-{自增id}，去手填。

## 1. 总体概况

| 维度 | 结果 |
|---|---|
| 后端 (temurin-8) | **527/527** ✅（+TC-PRJ 自动编号 / TC-PROJTYPE-011..015 / TC-INI 调整；0 fail）|
| 前端 | **248/248** ✅ + tsc clean + eslint 0 warn |
| E2E（docker 真 MySQL，前后端重建部署） | green |
| Step 0 评审（前台对抗式 agent，规避后台工作流中断） | 14 类核查 → **仅 1 L（陈旧注释，已修）**；两步保存事务安全/并发唯一/契约/数据安全均确认无虞 |
| 数据 | 既有项目 code 不动；E2E throwaway 项目（id 14–17）建即删 |

## 2. 改动
- 后端：`ProjectType` +PREFIXES(LT/CF/CT/ED)+`codePrefix()`；`ProjectService.create` 去 `existsByCode`/`req.getCode()`，临时占位→拿 id→回填 `{prefix}-{id}`（同事务两步保存）；`ProjectCreateRequest.code` 改可空且忽略；`OpportunityInitiateRequest` 去 `projectCode`；`resolveDeliveryProject` 内联新建仅 name+owner（code 自动生成）。
- 前端：`api/project.ts` `ProjectCreate.code` 可空；`api/opportunity.ts` 去 `projectCode`；ProjectsPage 去编号输入（列表仍只读展示）；DeliveryFlow 立项新建去编号输入。

## 3. E2E（live MySQL）
| 步骤 | 结果 |
|---|---|
| 创建 CASUAL/CORE_FEATURE/CORE_TECH/EXTERNAL_DELIVERY | code = LT-14 / CF-15 / CT-16 / ED-17 ✅ |
| 传入 code:"IGNORED-XYZ" | 被忽略、仍自动生成 ✅ |
| 立项内联新建（仅名称+负责人） | 新项目 code 匹配 `ED-{id}`、商机关联（后端 TC-INI-03 + v0.0.48 实测路径）✅ |

## 4. 失败模式 / 评审
- (k) 契约：前后端 initiate JSON 去 projectCode 两侧一致；ProjectType 值/前缀一致（E2E 实测）。
- 数据安全：backfill 仅改 project_type，不动 code；既有项目 code 保留。
- 评审确认：无 DB-UNIQUE on code → id 派生编号天然唯一、占位安全；两步保存在单事务内，占位不外泄、失败整体回滚。

## 5. 结论
✅ 项目编号自动生成（类型前缀 + 自增 id），去手填；立项内联新建去编号。后端 527/527 + 前端 248/248 + E2E 绿；评审 C:0 H:0 M:0 L:1（已修）。建议进入 Phase 6。
