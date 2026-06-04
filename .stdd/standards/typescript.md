# TypeScript 开发规范

> 适用版本：TypeScript 5.x
> 适用框架：React 18 + Vite 5 + React Router 6 + Zustand + Axios
> 最后更新：2026-06-04
> 参考实现：[`frontend/`](../../frontend/)（Rainier v0 bootstrap）

## 一、代码风格

### 1.1 格式化

- **格式化器**：Prettier 3（`singleQuote: true`、`trailingComma: 'all'`、`printWidth: 100`、`tabWidth: 2`、`semi: true`、`arrowParens: 'always'`、`endOfLine: 'lf'`）
- **静态检查**：ESLint 9 flat config（`@eslint/js` recommended + `typescript-eslint` recommended + `eslint-plugin-react` recommended + `eslint-plugin-react-hooks` recommended，最后 `eslint-config-prettier` 关闭与 Prettier 冲突的规则）
- **命令**：
  - lint：`npm run lint`（即 `eslint . --max-warnings 0`）
  - 单测：`npm test -- --run`
  - 生产构建：`npm run build`（即 `tsc -b && vite build`）
- **严格 TS**：`tsconfig.json` 必须含 `strict: true`、`noUnusedLocals: true`、`noUnusedParameters: true`、`noFallthroughCasesInSwitch: true`
- **文件编码**：UTF-8 / LF（统一由根 `.editorconfig` 约束）

### 1.2 命名约定

| 类型 | 规范 | 示例 |
|---|---|---|
| 源文件（含 JSX） | PascalCase `.tsx`（组件） / camelCase `.ts`（工具/store） | `Button.tsx`、`auth.ts`、`tokens.ts` |
| 测试文件 | 同源文件名 + `.test.tsx` / `.test.ts` | `Button.test.tsx`、`auth.test.ts` |
| 组件 / 类 / 类型 / Interface | PascalCase | `AuthUser`、`ProtectedRoute`、`LoginResponse` |
| 函数 / 变量 / props 字段 | camelCase | `setAuth`、`expirationHours`、`onSubmit` |
| 常量（真常量） | UPPER_SNAKE_CASE | `TOKEN_STORAGE_KEY` |
| Hook | `useXxx` | `useAuthStore` |
| CSS 类 / CSS 变量 | kebab-case + 项目前缀 `rainier-` | `.rainier-button`、`--rainier-color-primary` |

### 1.3 Import 顺序

由 ESLint + Prettier 自动管理：
1. 第三方库
2. 项目内绝对/相对路径
3. 样式 / 资源（CSS / SVG / 图片）

禁止 `import * as X from '...'` 类整体导入（除非确实需要命名空间）。

## 二、目录结构与分层

### 2.1 根结构

```
frontend/src
├── main.tsx                       // 应用入口（ReactDOM.createRoot）
├── App.tsx                        // 全局 Provider（BrowserRouter + ...）
├── AppRoutes.tsx                  // 路由树（测试侧可单独 import）
├── api/<feature>.ts               // HTTP 接口封装（薄）
├── store/<feature>.ts             // Zustand store
├── components/
│   ├── ui/{Button,Input,Card}.tsx // 设计系统基础组件
│   ├── AppLayout.tsx              // 全局壳（header + main）
│   └── ProtectedRoute.tsx         // 路由守卫
├── pages/<Feature>/
│   ├── index.tsx                  // 列表 / 主页面
│   ├── Detail.tsx                 // 详情（如有）
│   ├── EditDrawer.tsx             // 新建/编辑抽屉
│   └── <Feature>.css              // 页面级样式
├── styles/{tokens.ts, global.css} // 设计 token 注入
└── test/setup.ts                  // 全局测试 setup（含 polyfill）
```

**实例**：完整结构见 [`frontend/src/`](../../frontend/src/)。

### 2.2 分层规则

- **`api/*.ts`** 只做 HTTP ↔ TS 类型转换，依赖 [`api/client.ts`](../../frontend/src/api/client.ts) 单例；导出 `async function`，不在这里做副作用（状态、跳转）
- **`store/*.ts`** 用 Zustand 持有 UI / 业务状态；持久化字段用 `localStorage`（包装在 `try/catch`，见 [`store/auth.ts`](../../frontend/src/store/auth.ts)）；用 `useXxxStore.getState()` 在非 React 上下文（如 axios 拦截器）获取最新状态
- **`pages/*`** 持有 UI 状态（`useState`） + 调 `api/` + 调 `store/`；禁止直接 import `axios`，必须经 `api/client.ts`
- **`components/ui/*`** 是无业务的设计系统组件；样式只引 `--rainier-*` token，**禁止硬编码颜色 / 圆角 / 阴影**
- **`components/`** 跨页面共享的业务组件（如 `AppLayout`、`ProtectedRoute`）

### 2.3 设计 token

- 颜色 / 圆角 / 阴影 / 字体均通过 CSS 变量暴露，定义于 [`styles/global.css`](../../frontend/src/styles/global.css) 的 `:root`
- TS 侧镜像在 [`styles/tokens.ts`](../../frontend/src/styles/tokens.ts)（一次声明两处用）
- 组件 CSS 使用 `var(--rainier-color-primary)`，**禁止**直接写 `#3370FF`

## 三、类型与严格性

### 3.1 强制要求

- 所有公共 API（exported `function` / `class` / `const`）必须显式类型注解
- 内部 props / state 可依赖类型推断，但 `useState` 泛型推不出来时显式标 `useState<string | null>(null)`
- 优先 `interface`（结构化对象） vs `type`（联合 / 别名 / 函数签名）
- 禁止 `any`（test 文件例外，eslint config 已豁免）；用 `unknown` + 类型守卫

### 3.2 严格 null

- `strict` + `strictNullChecks` 已启用
- 访问可空值用可选链 `obj?.x?.y` 或显式 `if (x !== null && x !== undefined)`
- 必填 props 必须直接定义为非空类型，靠 `defaultProps` 或解构默认值

## 四、错误处理

### 4.1 HTTP 错误

- 全局 401 由 [`api/client.ts`](../../frontend/src/api/client.ts) 拦截器统一处理（清 store + 跳 `/login`）
- 页面 try/catch 时**必须用 `AxiosError` 类型守卫**区分 HTTP 错误与网络错误：
  ```ts
  import axios from 'axios';
  try { ... } catch (err) {
    if (axios.isAxiosError(err)) {
      const status = err.response?.status;
      // 区分 400 / 5xx
    } else {
      // 网络错误 / 其他
    }
  }
  ```
- 用户可见错误用中文友好信息；**不要把 `err.message` 直接抛给用户**

### 4.2 React 渲染错误

- 路由级 ErrorBoundary 由 React Router 提供（`errorElement`）；推荐每个 page 包一层
- 非 React 上下文（axios 拦截器、`setTimeout`）的异常应 `console.error` + 上报，不要静默

### 4.3 localStorage / sessionStorage

- 包装在 `try/catch`（隐私模式 / SSR / 禁用 storage 时会抛），见 [`store/auth.ts`](../../frontend/src/store/auth.ts) 的 `readInitialToken`

## 五、状态管理

### 5.1 Zustand 模式

- 一个 feature = 一个 store；store 文件不超过 200 行，逻辑大时拆 service 函数
- store action 内同步更新 + 副作用（如 `localStorage.setItem`）放一起，外部调用方不感知
- React 组件用 `useAuthStore((s) => s.token)` 单字段订阅，避免不必要 re-render
- 非 React 上下文用 `useAuthStore.getState()`

### 5.2 useState vs Zustand

- 仅当前页面 / 组件用：`useState`
- 跨页面共享 + 持久化 / 跨组件读写：Zustand store
- 服务端缓存（来自 API）：当前 v0 用 Zustand 兜底；后续如需要可引入 TanStack Query

## 六、测试代码规范

### 6.1 测试栈

- **Vitest 2.x**（与 Vite 同生态，`environment: 'jsdom'`、`globals: true`）
- **React Testing Library 16.x**（`render`、`screen`、`fireEvent`、`@testing-library/user-event`）
- **`@testing-library/jest-dom/vitest`**（断言扩展：`toBeInTheDocument()`、`toHaveTextContent()` 等）
- **jsdom 25.x** + 项目级 polyfill：[`src/test/setup.ts`](../../frontend/src/test/setup.ts) 提供 `MemoryStorage` 兜底 `window.localStorage` / `sessionStorage`（**jsdom 25 + Node 25 已知坑：Storage 方法缺失**）

### 6.2 测试文件组织

- 测试与源同目录：`Button.tsx` ↔ `Button.test.tsx`
- 全局 setup 在 [`src/test/setup.ts`](../../frontend/src/test/setup.ts)，由 `vite.config.ts` 的 `test.setupFiles` 拉起
- **JSX 测试文件必须用 `.tsx`**，纯逻辑测试可用 `.ts`（混淆易踩坑，统一 `.tsx` 也行）

### 6.3 测试命名

`describe('<被测对象>', () => { it('<scenario，期望主语用动词>', () => {...}) })`，与 spec Scenario 名直接对应。

**示例**（[`ProtectedRoute.test.tsx`](../../frontend/src/components/ProtectedRoute.test.tsx)）：

```tsx
describe('ProtectedRoute via full route tree', () => {
  it('redirects to /login when there is no token (TC-FES-001)', () => { ... });
  it('renders Home with username when authenticated (TC-FES-002)', () => { ... });
});
```

### 6.4 测试隔离（强制）

每个测试文件**必须**在 `beforeEach` 重置共享状态：

```ts
import { useAuthStore } from '@/store/auth';
beforeEach(() => {
  useAuthStore.setState({ token: null, user: null });
  window.localStorage.removeItem('rainier.token');
});
```

**反面教训**：v0 Phase 5 review 在 [`tokens.test.tsx`](../../frontend/src/styles/tokens.test.tsx) 命中过这个坑。

### 6.5 路由 / API mock

- 路由测试用 `<MemoryRouter initialEntries={['/...']}>` 包 `<AppRoutes />`，**不要包 `App`**（`App` 自带 `BrowserRouter`）
- API mock 优先用 `vi.mock('@/api/auth', () => ({...}))` 单测；端到端流程用 MSW（v0 已装但未启用，需要时在 setup.ts 起 `setupServer`）

### 6.6 断言强度

- 渲染断言：用 `getByTestId` / `getByRole`（无障碍优先），**少用** `getByText`（i18n 不稳）
- 样式断言：jsdom **不会展开** `var(--rainier-*)`，所以测 token 用 `:root` 的 `getPropertyValue('--rainier-...')`；测组件颜色断言 `style.backgroundColor === 'var(--rainier-color-primary)'`（证明引用 token，未硬编码）
- 异步断言用 `await waitFor(() => ...)` 或 `findByX`，禁止 `setTimeout` 等待

## 七、代码审查清单

> 由 STDD Phase 5 多代理 review 的 `code` agent 使用；逐项核查。

- [ ] **Bug 风险**：可空字段访问安全、Promise 链路 `await` 覆盖、`useEffect` 依赖完整、清理函数到位
- [ ] **死代码**：无 `console.log` 调试残留 / 未用 import / 未用 props / 注释掉的 JSX
- [ ] **一致性**：同 feature 内 api / store / page 模式一致；组件命名 PascalCase；测试命名遵循 §6.3
- [ ] **错误处理**：HTTP 错误用 `axios.isAxiosError` 类型守卫；用户可见消息中文友好；localStorage 调用包 try/catch
- [ ] **安全**：无 token 输出到 console / 日志；`dangerouslySetInnerHTML` 仅在已转义后使用；外部 URL 校验来源
- [ ] **测试覆盖**：每个 spec Scenario ≥ 1 测试；每个公共组件 ≥ 1 渲染测试；错误路径 / 边界状态 ≥ 1 测试
- [ ] **Fixture 质量**：所有 store + localStorage 测试有 `beforeEach` 清理；MSW 测试有 `server.resetHandlers()`
- [ ] **断言质量**：用 `getByTestId` / `getByRole` 而非脆弱的 `getByText`；样式断言考虑 jsdom 限制；时间断言用 `waitFor`

## 八、依赖与 package.json

- 新增依赖**先评估**：是否真需要、是否已有等价；优先选活跃维护的、与 Vite 生态相容的
- 区分 `dependencies`（运行时）vs `devDependencies`（仅构建/测试）
- 锁定大版本（`^x.y.z`），CI 用 `npm ci`（依赖 `package-lock.json`）
- 禁止 `latest` / `*` 作为版本号
- 引入"重"依赖（如完整 UI 组件库）必须经过 STDD 变更评审（v0 故意未引 Ant / Arco，避免视觉风格被锁）
