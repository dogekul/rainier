# Spec: C5 capability-tags

## Entities (NEW)

### `CapabilityTag` → `rainier_capability_tag`
- `id` (BaseEntity)
- `name` String NOT NULL, length 64, UNIQUE — 服务层校验，避免软删残留挡新增（沿用 Position.code 模式）
- `category` String NOT NULL, length 16 — 取值 `TECH` / `PRODUCT` / `SOFT`（服务层枚举校验，DB 仍存 String）
- 软删：是（`@SQLDelete` + `@Where("del_flag = 0")`）

### `UserCapability` → `rainier_user_capability`
- `id` (BaseEntity)
- `userId` Long NOT NULL
- `capabilityTagId` Long NOT NULL
- `level` Integer NOT NULL（1..5；服务层校验）
- `source` String NOT NULL, length 16 — `SELF` / `MANAGER`
- `@UniqueConstraint(userId, capabilityTagId)` —— 一个 user 对同一 tag 只有一条
- 软删：否（hard delete；同 `UserOrganization` 风格）

## Repositories
- `CapabilityTagRepository extends JpaRepository<CapabilityTag,Long>` + `boolean existsByName(String)`
- `UserCapabilityRepository extends JpaRepository<UserCapability,Long>`
  - `List<UserCapability> findByUserId(Long)`
  - `Optional<UserCapability> findByUserIdAndCapabilityTagId(Long, Long)`

## Service `CapabilityService`
- `List<CapabilityTagDto> listAllTags()` —— 按 category, name 排序
- `Map<String, List<CapabilityTagDto>> categorizeTags()` —— LinkedHashMap，key 是 category，便于前端分组
- `CapabilityTagDto createTag(name, category)`
  - 校验 category ∈ {TECH, PRODUCT, SOFT}
  - 校验 name 不重复（`existsByName`）
  - 否则抛 BadRequest / Conflict
- `List<UserCapabilityDto> listUserCapabilities(Long userId)` —— join tag, 输出含 tag name + category
- `UserCapabilityDto setUserCapability(Long userId, Long tagId, int level, String source)`
  - 校验 level ∈ [1,5]，source ∈ {SELF, MANAGER}
  - tag 存在（NotFound 否则）
  - upsert：findByUserIdAndCapabilityTagId 命中则更新 level/source，否则新建

## Endpoints
| Method | Path | Auth | 备注 |
|---|---|---|---|
| GET | `/api/capability-tags` | all-users（token-optional） | 列出 + 自带分组 `{flat, byCategory}` |
| POST | `/api/admin/capability-tags` | admin（AdminPaths Tier A `/api/admin`） | body: `{name, category}` |
| GET | `/api/me/capabilities` | token-gated（同 me/profile） | 当前用户能力列表 |
| POST | `/api/me/capabilities` | token-gated | body: `{capabilityTagId, level}`；source 强制 SELF |
| GET | `/api/users/{id}/capabilities` | 复用 C3 鉴权（self 或直接上级） | 同 `UserProfileController` 套路 |

`ProfileResponse` 增加 `List<UserCapabilitySummary> capabilities` 字段（同 shape，level + tagName + tagCategory + source）。已有调用方不需要变更（默认空列表）。

## Seed
- `CapabilityTagSeed` (`CommandLineRunner`, `@Order(HIGHEST_PRECEDENCE)`)，flag `app.demo.capability-seed.enabled` 默认 true，test profile 关。
- 表为空才插，幂等。
- 10 个 tag：
  - TECH: Java, Frontend, K8s, SQL
  - PRODUCT: 用户研究, 需求分析, 数据分析
  - SOFT: 沟通, 跨团队协作, 带人

## 鉴权落点
- `AdminPaths.TIER_A` 已含 `/api/admin` 前缀；新建 `POST /api/admin/capability-tags` 自动被拦截，无需改 AdminPaths。
- `GET /api/capability-tags` 不在 AdminPaths 名单 —— 默认 all-users。

## Test Cases

### CapabilityServiceTest
- TC-CAP-001: createTag 正常 → 返回带 id 的 dto，listAll 含之
- TC-CAP-002: createTag 重名 → ConflictException
- TC-CAP-003: createTag category 非法 → BadRequestException
- TC-CAP-004: setUserCapability 首次 → 新建
- TC-CAP-005: setUserCapability 再次同 tag → 更新（level/source 变；不新增行）
- TC-CAP-006: setUserCapability level=0 或 6 → BadRequestException
- TC-CAP-007: setUserCapability source=OTHER → BadRequestException
- TC-CAP-008: setUserCapability tagId 不存在 → NotFoundException
- TC-CAP-009: listUserCapabilities → 返回 join 后的 dto（带 tagName + category）
- TC-CAP-010: categorizeTags → 按 TECH/PRODUCT/SOFT 分桶且 each 内有序

### CapabilitySeedTest
- TC-CAP-SEED-001: 启用 seed → 表中至少 10 条，category 覆盖 TECH/PRODUCT/SOFT
- TC-CAP-SEED-002: 二次 run 幂等，count 不变

## Out of Scope
- AI 自动归因
- Position ↔ Tag 映射
- 由 manager 给下属打 level（端点已支持 source=MANAGER，但无独立 endpoint）
- 标签删除 / 重命名
- 标签层级 / 别名
- 前端 UI
