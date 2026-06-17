# v0.0.29-portfolio-consumers — 跨项目 cockpit + 修团队面板作用域

> Baseline: tag `v0.0.28-scope-substrate`. 结构层第二波 cycle #2 — 消费 portfolio 底座,首批可见。

## Why
v0.0.28 建了 `/api/me/portfolio` 读模型但还不可见。这一版让它显形:给 PM 跨项目总览,并修掉团队负责人面板
「RYG 挂个人项目而非团队足迹」的 mis-scope。

## What Changes (frontend only)
- board-kit `StatusChip` 加可选 `tier` prop(直接按 RYG 上色);board utils 加 `rygToTier`/`RYG_LABEL`。
- **Cockpit 跨项目健康总览条**:顶部新增「我的项目（健康总览）」—— `getPortfolio('mine')` 的红黄绿迷你卡
  (worst-first),点一张即把下方单项目详情切到该项目。PM 0 点击见全部项目健康,1 点击钻进任意一个。
- **TeamLead 项目红黄绿改用 `getPortfolio('led')`**:团队足迹(我带的组织+子树下挂的项目),服务端算 RYG
  + worst-first,取代原先用 lead 个人 `user.projects` 客户端 fan-out 的错作用域。

## Success Criteria
- [ ] Cockpit 顶部健康条:多项目时渲染,RYG 上色,点卡切项目并重载。
- [ ] TeamLead 项目卡来自 scope=led(不再是个人项目);RYG/排序正确。
- [ ] tsc/eslint/vitest green(146→149)。
