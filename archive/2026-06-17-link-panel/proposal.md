# v0.0.31-link-panel — 关联面板 (structured external-artifact links)

> Baseline: tag `v0.0.30-portfolio-map`. 路线图 #6 — 6 角色点名的核心动作 + AI 证据底座。

## Why
PO/架构师/测试/开发 都点名「关联外部产物」是缺失的核心动作(PRD/设计稿/缺陷/用例/PR 都在外部工具)。今天
Story/Task 零 url 字段,人们把链接塞进 description。一个极简多态 link 实体一次服务 6 角色,也是 AI evidence_refs 的底座。

## What Changes
- NEW backend `entity-link`: `EntityLink`(targetType STORY/TASK + targetId + linkType PRD/DESIGN/DEFECT/
  TESTCASE/PR/OTHER + label + url, 表 rainier_entity_link, 软删)。`POST/GET ?targetType=&targetId=/DELETE`
  /api/links;service 校验 targetType/linkType + url NotBlank。
- 前端 `api/link.ts` + 极简 `LinkPanel`(type chip + url + 删除 + 一行新增[type+url,label 可选] + 计数)。
- 接入 `TaskEditDrawer`(targetType=TASK)+ `StoryEditDrawer`(targetType=STORY),编辑既有实体时显示。
- **极简**(只 url+type,符合卡片「关联必须极简否则没人维护」)。

## Success Criteria
- [ ] POST 校验 targetType/linkType/url;GET 按 target 过滤 oldest-first;DELETE 软删。
- [ ] LinkPanel 渲染既有链接 + 计数;新增携 type+url;删除生效。Task/Story 抽屉接入。
- [ ] backend 395→401 green(20 表);frontend 153→156;tsc/eslint/checkstyle clean。
