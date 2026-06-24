# Design: v0.0.49 — 项目编号自动生成

基线 tag v0.0.48-project-types / commit c23f441。

## Context

- `Project.code` = `@Column(nullable=false, length=64)`，**无 DB UNIQUE**（service 层 `existsByCode` 查重，同 Requirement/Position 范式）。
- `ProjectService.create`：现校验 owner、`existsByCode`、status、projectType，然后 `p.setCode(req.getCode())` 保存。
- `code` 由 IDENTITY 自增 id 决定 → 必须**先 insert 拿 id、再回填 code**（两步保存，同事务）。
- 立项内联新建（v0.0.48）经 `OpportunityInitiateRequest.projectCode/projectName/projectOwnerUserId` → `ProjectService.create`。

## Decisions

### D1 类型前缀（ProjectType）
新增 `Map<String,String> PREFIXES`：CASUAL=`LT`、CORE_FEATURE=`CF`、CORE_TECH=`CT`、EXTERNAL_DELIVERY=`ED`；`codePrefix(type)` 返回前缀（未知兜底 `PRJ`）。

### D2 自动生成（ProjectService.create）
- 去掉 `existsByCode`（id 天然唯一）与 `setCode(req.getCode())`；忽略请求 code。
- 流程：实体设非 code 字段 + 临时占位 code（无 DB UNIQUE，占位安全）→ `saveAndFlush` 拿 id → `setCode(codePrefix(type)+"-"+id)` → `saveAndFlush`。同事务原子，占位 code 不外泄。
- **备选**：用单独序列号。排除——用户明确要「项目自增ID」，复用 id 最简且唯一。

### D3 ProjectCreateRequest.code 可空且忽略
- 去 `@NotBlank`（保留字段、文档标「服务端生成，输入忽略」），降低对既有调用方/测试的破坏面（未知/多余字段 Jackson 默认忽略）。

### D4 立项内联新建去 code（opportunity）
- `OpportunityInitiateRequest` 移除 `projectCode`（v0.0.48 新增、消费方少，清理干净）。
- `resolveDeliveryProject`：`hasCreate = !isBlank(projectName)`；新建仅校验 projectName（+owner 兜底）；构造 `ProjectCreateRequest` 仅 name+owner+type（code 自动生成）。

### D5 前端
- `api/project.ts`：`ProjectCreate.code` 改可空（不再发送）；`Project.code` 保留（只读展示自动编号）。
- ProjectsPage：去掉编号输入框 + code state；创建 body 不带 code；列表/详情仍显示 `r.code`。
- `api/opportunity.ts`：`OpportunityInitiate` 去 `projectCode`。
- DeliveryFlow：立项新建表单去编号输入，仅 名称 + 负责人；doHandoff 创建 body 去 projectCode；校验仅名称。

## Risks / Trade-offs

| 风险 | 缓解 |
|---|---|
| 两步保存：占位 code 与并发 | 无 DB UNIQUE → 占位不冲突；同事务最终落 prefix-id |
| 既有断言 code 具体值的测试（ProjectControllerCreateTest code=PROJ-001） | 改断言为自动编号格式（`^LT-\d+$` 等） |
| 多余 code/projectCode 字段残留在测试 JSON | Jackson 默认忽略未知属性（Boot FAIL_ON_UNKNOWN_PROPERTIES=false），无害；关键测试更新 |
| 类型后改 → code 不变 | 设计即「创建时定、不可变」，符合稳定标识预期 |
