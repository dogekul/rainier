# v0.0.16-project-type 切片执行计划

> 8 切片 P01-P08，拓扑分批。entity-field-add 类型，小变更。

| # | 优先级 | TC 覆盖 | 实现目标 | 依赖 |
|---|--------|---------|---------|------|
| P01 | P0 | (基础) | `ProjectType` 常量类(CASUAL/FORMAL+ALL，照搬 ProjectStatus) + `Project` 加 `projectType` nullable 列 + getter/setter | 无 |
| P02 | P0 | TC-PROJTYPE-010 | 3 DTO 透传：`ProjectCreateRequest`(@Size≤16 可选) / `ProjectUpdateRequest`(@Size≤16 可选) / `ProjectDetail`(字段 + `from` null→CASUAL 兜底) | P01 |
| P03 | P0 | TC-001..008 | `ProjectService`：create 默认CASUAL+ALL.contains校验 / update present-validate-set+absent-preserve / list 加 projectType filter；`ProjectController` list 加 `@RequestParam projectType` | P01,P02 |
| P04 | P0 | TC-009 | `ProjectTypeBackfill` runner(CommandLineRunner+@Order HIGHEST+native UPDATE ... WHERE project_type IS NULL) | P01 |
| P05 | P0 | TC-001..010 | backend 测试：controller 集成(create默认/显式/非法、update转化/保留/非法、list过滤、detail字段) + 回填 runner 单测 + DTO 兜底单测；**同步**既有 detail 字段集 exact-equality 测试加 projectType | P01-P04 |
| P06 | P0 | (前端基础) | `api/project.ts`：`ProjectType` 联合类型 + `projectType` 加到 Project/ProjectCreate/ProjectUpdate/ProjectListParams | 无(可并行) |
| P07 | P0 | TC-FES-001..004 | `ProjectsPage`：抽屉类型 select(默认CASUAL，编辑回显) + 表格类型列(中文) + 类型过滤 select(扩展 fetcher)；`ProjectsPage.test` 新增 4 用例 + 同步既有断言 | P06 |
| P08 | P0 | TC-E2E-001 | docker 重建 + 启动回填验证(现有 project→CASUAL 且其它字段不变) + 转化链(POST FORMAL/PUT 转化/GET ?projectType 过滤) + SHOW TABLES=18 + 审计白拿验证 | P01-P07 |

## 拓扑批次

- 批次 1：P01(domain) ‖ P06(前端 api)
- 批次 2：P02(DTO) ‖ P04(backfill)
- 批次 3：P03(service+controller)
- 批次 4：P05(backend 测试) ‖ P07(前端页+测试)
- 批次 5：P08(E2E)

## 陷阱清单(给 BUILD)

- A: Java 8 — ProjectType.ALL 用 `Collections.unmodifiableSet(new HashSet<>(Arrays.asList(...)))`。
- D4: update absent→保留(不降级)；present→校验+set；非法→`BadRequestException("invalid project type: ...")`。
- D7: 读兜底放 `ProjectDetail.from`；回填 runner native SQL。
- 字段集: 既有 detail 字段集 exact-equality 测试**必须同步**加 projectType（否则假红）。
- H2: test create-drop 无存量 → 回填测试手动注入 null 行(native UPDATE set null)再调 runner。
- 前端: ProjectsPage.test 既有断言若硬编码列/字段，grep 校对同步。
- 契约 K: 后端 ProjectDetail.projectType 字段名 == 前端 Project.projectType；list param 名 projectType 一致。
