# 补齐 `.stdd/standards/{java,typescript}.md` 开发规范

## Why

V0 bootstrap 收尾时记录的 [Adjustment #2](../../archive/2026-06-02-bootstrap-fullstack-scaffold/design-adjustments.md) 显示：`.stdd/standards/` 仅含 `python.md`，本项目涉及的 Java 与 TypeScript 均无对应规范文件。SLICE-B07（Spotless+Checkstyle）和 SLICE-F05（ESLint+Prettier）只能用 `design.md §10` 的工具链选择临时兜底，没有沉淀成可复用的开发规范。

下一步要做 10 个实体变更，每个变更都会重复"决定命名、决定分层、决定 lint 严苛度、决定测试约定"——若每次现凑，团队风格会漂移；Phase 5 多代理 review 也没有稳定基线可对照。本变更补齐这两份规范，让后续每个实体 STDD 变更的 Phase 4 BUILD 步骤 0（"必须先读取开发规范"）真正可执行。

## What Changes

- 新建 `.stdd/standards/java.md`，结构对齐 `python.md` 的 7 节模板（代码风格 / 类型 / 异步 / 错误处理 / 日志 / 测试规范 / 审查清单），内容**以 v0 bootstrap 实际落地的工具链为准**（Spring Boot 2.7 + Java 8 + Spotless GoogleJavaFormat **GOOGLE** style + 自定义最小 Checkstyle + JUnit 5 + Mockito + AssertJ + MockMvc 切片）
- 新建 `.stdd/standards/typescript.md`，同样 7 节，内容覆盖 React 18 + Vite + TS 5 + Zustand + Axios + ESLint flat config + Prettier + Vitest + RTL + jsdom（含 v0 用到的 `MemoryStorage` polyfill）
- 两份标准均**引用 v0 实际代码位置**（`backend/src/main/java/com/rainier/...`、`frontend/src/...`）作为可视示例，不是凭空理论
- 临时放开 `.claude/settings.local.json` 中对 `.stdd/standards/**` 的 `Write`/`Edit` 禁令（仅本变更需要，交付时回写该禁令以保护 STDD 系统文件）
- 不修改 `python.md`（不重写、不格式化、不重排）
- 关闭 v0 bootstrap 的 Adjustment #2（在本变更的 `pending-adjustments.md` 中标注"已解决"）

## Capabilities

### Modified Capabilities

- （无产品代码变更）

### New Capabilities

- `dev-standard-java`：Java 开发规范（流程层 capability，被 STDD Phase 4 引用）
- `dev-standard-typescript`：TypeScript 开发规范（同上）

## Impact

**代码层面**：

- 新增 2 个文件：`.stdd/standards/java.md`、`.stdd/standards/typescript.md`（各约 150–200 行，参考 `python.md` 145 行规模）
- 修改 1 个文件：`.claude/settings.local.json`（临时调整 deny 列表，交付时恢复）
- 不修改 `backend/`、`frontend/` 任何源码
- 不修改 `archive/2026-06-02-bootstrap-fullstack-scaffold/`（已归档不动）

**配置层面**：

- 无（本变更不引入新工具、不改 `pom`/`package`/`eslint` 配置）

**基础设施**：

- 无

**流程层面**：

- 未来 STDD 变更的 Phase 4 起点（`stdd-build` Skill Step 0）会先读这两份标准
- Phase 5 多代理 review 的 `code` agent 会以这两份标准为审查基准（而非 ad-hoc 推断）

## Success Criteria

- [ ] `.stdd/standards/java.md` 存在；7 节齐全；每节内容反映 v0 bootstrap 实际选择（非 design.md §10 原文）
- [ ] `.stdd/standards/typescript.md` 存在；7 节齐全；同上
- [ ] 两份标准的"代码风格"节直接给出 `mvn` / `npm` 命令；不需要读者再去查 pom/package
- [ ] 两份标准的"测试规范"节明确：测试命名 / 文件位置 / 测试隔离（含 jsdom localStorage polyfill 已知坑）/ 断言强度
- [ ] 两份标准的"审查清单"节覆盖 STDD Phase 5 Step 0 的 8 个维度（bug 风险 / 死代码 / 一致性 / 错误处理 / 安全 / 测试覆盖 / fixture 质量 / 断言质量）
- [ ] 两份标准均引用 v0 实际文件路径作为示例（grep `backend/src/.../*.java` 或 `frontend/src/.../*.tsx` 至少各 2 处）
- [ ] `python.md` 一字不改（git diff 显示该文件未变）
- [ ] 本变更交付时 `.claude/settings.local.json` 中 `Write(.stdd/standards/**)` / `Edit(.stdd/standards/**)` deny 已恢复
- [ ] `archive/2026-06-02-bootstrap-fullstack-scaffold/pending-adjustments.md` 中 Adjustment #2 标注为"已解决，见 changes/2026-06-04-add-language-standards"
