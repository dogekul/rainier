# Capability: entity-link

> Change log:
> - 2026-06-17 (v0.0.31-link-panel) — NEW. 关联面板: a deliberately minimal external-artifact link
>   attached to a Story or Task (`EntityLink`: targetType STORY/TASK + targetId + linkType
>   PRD/DESIGN/DEFECT/TESTCASE/PR/OTHER + label + url; table `rainier_entity_link`, soft-delete). The
>   PM system relates external products (PRD/设计稿/缺陷/用例/PR live in Confluence/Figma/禅道/GitLab),
>   never hosts them. Kept url+type-only per the role cards' 「关联必须极简否则没人维护」 trap. This is
>   also the evidence base the future AI Agent's `evidence_refs` will sit on. All-users; no update
>   endpoint (a link is delete-and-readd).

## ADDED Requirements

### Requirement: attach / list / remove external-artifact links

后端 SHALL 通过 `POST /api/links` 创建链接(校验 targetType ∈ {STORY,TASK}、linkType ∈ 六类、url NotBlank),
`GET /api/links?targetType=&targetId=` 按目标列出(创建时间正序),`DELETE /api/links/{id}` 软删。

#### Scenario: 创建合法 STORY 链接

- **GIVEN** `POST /api/links` body `{targetType:"STORY", targetId:5, linkType:"PRD", url:"https://...", label:"需求文档"}`
- **WHEN** 创建
- **THEN** SHALL 返回 201,body 含 targetType/targetId/linkType/label/url

#### Scenario: 非法 targetType / linkType / 缺 url 被拒

- **GIVEN** targetType="EPIC"(或 linkType="WHAT",或缺 url)
- **WHEN** `POST /api/links`
- **THEN** SHALL 返回 400

#### Scenario: 按目标过滤列出

- **GIVEN** Task 7 有两个链接、Task 99 有一个
- **WHEN** `GET /api/links?targetType=TASK&targetId=7`
- **THEN** SHALL 仅返回 Task 7 的两个链接,按创建时间正序

#### Scenario: 删除

- **GIVEN** 一个既有链接 id=L
- **WHEN** `DELETE /api/links/L`
- **THEN** SHALL 返回 204,后续列出 SHALL 不含 L

### Requirement: 前端关联面板 (frontend-scaffold 协同)

前端 `LinkPanel` SHALL 在 Task/Story 编辑抽屉(编辑既有实体时)展示该实体的链接列表(类型 chip + url + 删除)
+ 一行新增(类型 + url,说明可选)+ 计数,极简。
