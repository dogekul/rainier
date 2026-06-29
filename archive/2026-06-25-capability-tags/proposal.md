# Proposal: C5 capability-tags (基础版)

## 背景
- 已有 `Position`（岗位池 + Position.category），描述「正式岗位」。
- 没有「能力标签」概念 —— 无法回答「谁会 K8s / 谁懂用户研究 / 谁能带人」。
- 体验关心两件事：a) 个人主页能看到自己的能力维度；b) 后续匹配/推荐有锚点。

## 范围（v0.0.85）
- 新增 `CapabilityTag`（全局标签词典）+ `UserCapability`（user↔tag 的等级关系）两个 entity。
- 标签词典只读 + admin 创建（Tier A）；普通用户只能自评自己的能力。
- `ProfileResponse` 多一个 `capabilities[]` 字段（不破坏既有契约）。
- Demo seed 10 个常见标签，flag-gated（test 默认关）。

## OutOfScope
- AI 自动归因（依据 commit/Story 推断能力）—— 后续 sub-change
- Position ↔ Tag 映射（"PM 岗位推荐这些 tag"）—— 后续
- 由 manager 评估下属（source=MANAGER）已留接口、UI 后续
- 删除标签 / 重命名标签
- 标签层级 / 同义词
