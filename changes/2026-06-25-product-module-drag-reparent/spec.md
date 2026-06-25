# Spec — Product Module Drag Reparent UI

## Scenario 1: 拖到另一节点 → 成为其子
GIVEN tree M1（root）/ M2（M1 子）/ M4（root），三者同 productId=1
WHEN 用户把 M4 拖到 M2 行
THEN 调用 updateProductModule(4, { parentId: 2, ... }) 一次

## Scenario 2: 拖到根 dropzone → parentId=null
GIVEN M2 当前 parentId=1
WHEN 用户把 M2 拖到 "拖到此处成为顶级" dropzone
THEN 调用 updateProductModule(2, { parentId: null, ... }) 一次

## Scenario 3: 跨 productId 短路
GIVEN M1 (productId=1)，M5 (productId=2)
WHEN 用户把 M5 拖到 M1
THEN 不调用 updateProductModule（前端 effect=none，drop 被阻止）

## Scenario 4: 拖到自身/后代短路
GIVEN M1 → M2 → M3
WHEN 用户把 M1 拖到 M3
THEN 不调用 updateProductModule（成环防护）

## TestCases
- TC-FES-PMOD-DND-001: drop onto sibling root → PUT 调用一次，parentId 为目标 id
- TC-FES-PMOD-DND-002: drop onto root dropzone → PUT parentId=null
- TC-FES-PMOD-DND-003: cross-product drop → 0 次 PUT
- TC-FES-PMOD-DND-004: drop onto descendant → 0 次 PUT
