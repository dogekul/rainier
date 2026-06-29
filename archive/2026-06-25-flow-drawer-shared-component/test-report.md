# test-report — D8 ArtifactSupplementDrawer

## 范围
仅前端组件抽离 + Presale/Delivery 调用方改造。后端无改动。

## 命令
```bash
cd frontend && npx tsc --noEmit            # 干净
cd frontend && npm test -- --run            # 57 files / 280 tests passed
```

## 结果
| 套件 | 结果 |
|---|---|
| `src/components/flow/ArtifactSupplementDrawer.test.tsx` (NEW) | 5/5 passed |
| `src/pages/Crm/PresaleFlow.test.tsx` (回归) | 16/16 passed |
| `src/pages/Crm/DeliveryFlow.test.tsx` (回归) | 11/11 passed |
| `src/AppRoutes.test.tsx` (回归) | 23/23 passed |
| **全前端套件** | **280/280 passed** |

## 覆盖映射
| Scenario | 测试 |
|---|---|
| TC-D8-01 关时不渲染 | `does not render fields when opportunityId is null` |
| TC-D8-02 报告校验 | `blocks submit when a required report body is empty` |
| TC-D8-03 链接校验 | `blocks submit when a required link list is empty` |
| TC-D8-04 双类型提交 + onAdvance | `persists every missing artifact then calls onAdvance with the id` |
| TC-D8-05 链接增删 | `supports multiple links per artifact (add / remove)` |

## 回归保障
两份既有 page 测试覆盖了「missing 检测 → 抽屉打开 → 提交 → advance」端到端流程；保留了所有 data-testid（`presale-supp-*` / `delivery-supp-*`）→ 既有 27 项测试无需改动即全绿，证明对外行为契约不变。

## Caveats
- 仅抽离了真正重复的「补充产出物」抽屉；spec 原文提及的 `FlowDetailDrawer / StageProgressBar / ArtifactList` 在当前代码中没有对应的"详情抽屉"调用方（PresaleFlow/DeliveryFlow 都是 list 页 + 独立专用抽屉），强行新建会留下无 caller 的死组件，故未做。详情见 proposal「范围外」。
