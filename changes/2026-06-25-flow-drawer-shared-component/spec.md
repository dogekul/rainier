# spec — ArtifactSupplementDrawer 共用组件

## 组件契约
`<ArtifactSupplementDrawer>` 渲染「补充缺失产出物 → 推进商机」抽屉。

### Props
| prop | type | 说明 |
|---|---|---|
| `opportunityId` | `number \| null` | `null` = 关；非空 = 开 + 绑定该商机 |
| `missingTypes` | `ArtifactType[]` | 父算出的缺件列表；变化时内部 form 自动重置 |
| `message` | `string?` | 顶部 banner 文案，默认「推进前需补齐以下必需产出物：」 |
| `testIdPrefix` | `string` | data-testid 命名空间（e.g. `"presale-supp"` / `"delivery-supp"`） |
| `onClose` | `() => void` | 取消按钮 / 抽屉关闭回调 |
| `onAdvance` | `(id: number) => void \| Promise<void>` | 全部 artifact 持久化成功后调用；父负责关抽屉 + 推进 |

### data-testid 约定
- 报告类（非 link）：`{prefix}-{type}` / `{prefix}-title-{type}` / `{prefix}-content-{type}`
- 链接类：`{prefix}-{type}` / `{prefix}-link-{type}-{idx}` / `{prefix}-addlink-{type}` / `{prefix}-rmlink-{type}-{idx}`
- 共用：`{prefix}-save` / `{prefix}-error`

## 行为
1. **重置**：每次 `missingTypes` 字符串拼接发生变化（即父开了新的一次推进），内部 form data 重置。
2. **校验**：提交时遍历 `missingTypes`；报告类 `content` 必填，链接类至少 1 条非空 URL。失败 → `{prefix}-error` 出错信息。
3. **提交**：通过校验后逐个 `createOpportunityArtifact(opportunityId, payload)`：
   - 链接类：每条非空 URL 单独建一条
   - 报告类：`title` 空字符串 → `undefined`；`content` 原样上传
4. **回调**：全部成功 → `await onAdvance(opportunityId)`；任一失败 → 错误条 + 解除 saving
5. **链接增删**：链接类至少保留 1 条空行；删除最后一条时回退为 `['']`

## Scenarios
- TC-D8-01 父传 `opportunityId=null` → 表单字段不渲染
- TC-D8-02 报告类必填 `content` 缺失 → 校验错误，未调 API，未调 `onAdvance`
- TC-D8-03 链接类全空 → 校验错误，未调 API
- TC-D8-04 双类型（报告 + 链接）填齐 → 各调一次 API + `onAdvance(id)` 被以正确 id 调用
- TC-D8-05 链接类「+ 添加链接」生成新行；删除新行后还原

## 集成点
- `PresaleFlow.tsx`：原 `suppOpp` 触发 multi-doc 推进时改 mount `<ArtifactSupplementDrawer testIdPrefix="presale-supp" onAdvance={id => advance(id, suppDecision)} />`
- `DeliveryFlow.tsx`：现场调研推进同理，`message="推进前需补齐以下现场调研产出物："` + `testIdPrefix="delivery-supp"`

两份既有 page-level 测试（共 27 项）保持不变即回归通过 —— 共用组件保留了所有 data-testid。
