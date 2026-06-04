# Capability: frontend-scaffold

## ADDED Requirements

### Requirement: 路由守卫保护需登录页面

前端 SHALL 在未登录时阻止访问受保护路由，并将用户重定向至 `/login`。

#### Scenario: 未登录访问首页时重定向

- **GIVEN** 浏览器 localStorage 不含有效 `rainier.token`
- **WHEN** 用户访问 `/`
- **THEN** 系统 SHALL 渲染 `/login` 页面
- **AND** 浏览器地址栏 SHALL 为 `/login`

#### Scenario: 登录后访问受保护路由通过

- **GIVEN** localStorage 中存在有效 `rainier.token` 且 Zustand store 中 `user.username` 为 `alice`
- **WHEN** 用户访问 `/`
- **THEN** 系统 SHALL 渲染首页（Home 组件）
- **AND** 页面 SHALL 在右上角显示文本 `alice`
- **AND** 页面主区域 SHALL 显示文本 `Hello, alice`

### Requirement: 应用飞书风格全局主题

前端 SHALL 在 `:root` 注入飞书风格 design tokens（CSS variables），所有按钮 / 输入框 / 卡片组件 SHALL 引用 tokens 而非硬编码颜色。

#### Scenario: 主题 tokens 在 DOM 上可见

- **GIVEN** 前端应用已挂载到 `#root`
- **WHEN** 测试代码读取 `document.documentElement` 的 computed style
- **THEN** 系统 SHALL 暴露 CSS 变量 `--rainier-color-primary` 值为 `#3370FF`
- **AND** 系统 SHALL 暴露 `--rainier-radius-button` 值为 `6px`
- **AND** 系统 SHALL 暴露 `--rainier-radius-card` 值为 `8px`

#### Scenario: 登录按钮使用主色

- **GIVEN** 用户位于 `/login` 页面
- **WHEN** 测试通过 `getComputedStyle` 读取登录按钮的 `background-color`
- **THEN** 系统 SHALL 返回与主色 `#3370FF` 等价的颜色（`rgb(51, 112, 255)`）
