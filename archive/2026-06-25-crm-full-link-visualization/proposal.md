# proposal — D6 Opportunity↔Project↔Operation 全链可视化 (v0.0.94)

## 背景
售前→交付→运营 三段实体（Opportunity / Project / Operation）已分别落库，
但没有一处「站在商机视角看全链」的入口。详情页缺一个能一次性看到：
- 商机自身核心信息
- 立项后建出的 Project（含 stage 进展 = SURVEY..ACCEPTANCE 之活动/产出物 summary）
- 验收后接管的 Operation
- 关联的 Customer

的可视化。

## 范围
1. **后端**：新增 `GET /api/opportunities/{id}/full-link`
   - 返回 `FullLinkResponse`：opportunity / customer / project / operation
     / presaleStages[] / deliveryStages[] 概览（每段含 code/label/activityCount/artifactCount/done）
2. **前端**：详情页加「全链」面板（页面内 section，无 routing-level tab）
   - 以 Timeline（自实现，CSS 圆点+连线）渲染 商机→项目→运营 三段串联
   - 每段卡片显示关键属性 + 跳转链接
3. **测试**：`FullLinkServiceTest` — seed 完整链 → 校验全字段

## 不在范围
- 反向：项目找商机（项目页加 link，后续做）
- 图表化（Echart 等）
- Mantine Timeline（项目栈无 Mantine —— 用项目自有 CSS Timeline 取等价 UX）
