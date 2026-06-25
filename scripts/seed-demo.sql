/* v0.0.63 — wipe + reseed demo dataset covering all CRM/PM/OPS stages.
 *
 * Cleanup keeps dimension data (users 1-7, customers 4-9, product 1, product_module 1,
 * roles 1-2, position 1, organizations 1-6). Wipes:
 *   - all transactions (opps/artifacts/operations/issues/demands/requirements/sprints/
 *     stories/tasks/sprint_features/milestones/features/dr_links/projects/audit_log)
 *   - test customers (26-28), test products (4-6), test product_modules (5-8)
 *
 * Then seeds a comprehensive demo:
 *   - 10 opportunities one-per-stage (LEAD → ACCEPTANCE)
 *   - 3 operations one-per-stage (MAINTENANCE/OPERATING/REPURCHASE) + 5 issues
 *   - 4 projects (1 internal + 3 external-delivery from CRM)
 *   - 3 features under existing product/module
 *   - 5 requirements (one per status)
 *   - 4 sprints / 8 stories / 16 tasks
 *   - 4 milestones across two delivery projects
 *   - 6 demands (mix of pending/converted)
 *   - 2 dr_link rows
 */
SET FOREIGN_KEY_CHECKS = 0;

/* ─────────────────────────────────────────── cleanup ─────────────────────────────────────────── */

DELETE FROM rainier_operation_issue;
DELETE FROM rainier_operation;
DELETE FROM rainier_demand_requirement;
DELETE FROM rainier_task;
DELETE FROM rainier_story;
DELETE FROM rainier_sprint_feature;
DELETE FROM rainier_sprint;
DELETE FROM rainier_milestone;
DELETE FROM rainier_requirement;
DELETE FROM rainier_demand;
DELETE FROM rainier_opportunity_artifact;
DELETE FROM rainier_opportunity;
DELETE FROM rainier_feature;
/* v0.0.64 — wipe member + org_pmo before project (org_pmo references organization, project references organization). */
DELETE FROM rainier_project_member;
DELETE FROM rainier_organization_pmo;
DELETE FROM rainier_project;
DELETE FROM rainier_entity_link;
DELETE FROM rainier_audit_log;

DELETE FROM rainier_customer       WHERE id NOT IN (4,5,6,7,8,9);
DELETE FROM rainier_product        WHERE id <> 1;
DELETE FROM rainier_product_module WHERE id <> 1;
DELETE FROM rainier_ai_work_log;

ALTER TABLE rainier_opportunity          AUTO_INCREMENT = 1;
ALTER TABLE rainier_opportunity_artifact AUTO_INCREMENT = 1;
ALTER TABLE rainier_operation            AUTO_INCREMENT = 1;
ALTER TABLE rainier_operation_issue      AUTO_INCREMENT = 1;
ALTER TABLE rainier_project              AUTO_INCREMENT = 1;
ALTER TABLE rainier_requirement          AUTO_INCREMENT = 1;
ALTER TABLE rainier_sprint               AUTO_INCREMENT = 1;
ALTER TABLE rainier_story                AUTO_INCREMENT = 1;
ALTER TABLE rainier_task                 AUTO_INCREMENT = 1;
ALTER TABLE rainier_feature              AUTO_INCREMENT = 1;
ALTER TABLE rainier_milestone            AUTO_INCREMENT = 1;
ALTER TABLE rainier_project_member       AUTO_INCREMENT = 1;
ALTER TABLE rainier_organization_pmo     AUTO_INCREMENT = 1;
ALTER TABLE rainier_demand               AUTO_INCREMENT = 1;
ALTER TABLE rainier_demand_requirement   AUTO_INCREMENT = 1;
ALTER TABLE rainier_sprint_feature       AUTO_INCREMENT = 1;

SET @now := NOW(6);
SET @actor := 'alice';

/* ─────────────────────────────────────── features (3) ────────────────────────────────────────── */
/* Under the existing PROD-PAY / MOD-WALLET (id=1) module. */

INSERT INTO rainier_feature (id, code, name, description, module_id, owner_user_id, status, create_by, create_time, update_by, update_time, del_flag)
VALUES
  (1, 'FEAT-RECHARGE', '充值', '钱包账户充值能力，含支付宝/微信/银行卡多通道', 1, 3, 'ACTIVE',     @actor, @now, @actor, @now, 0),
  (2, 'FEAT-WITHDRAW', '提现', '钱包账户提现到银行卡', 1, 3, 'ACTIVE',     @actor, @now, @actor, @now, 0),
  (3, 'FEAT-BALANCE',  '余额查询', '账户余额/明细查询接口', 1, 4, 'PLANNING', @actor, @now, @actor, @now, 0);

ALTER TABLE rainier_feature AUTO_INCREMENT = 100;

/* ──────────────────────────────────── opportunities (10) ─────────────────────────────────────── */
/* Owner conventions:
 *   commercialOwner = 黎立 (id=2)     solutionOwner = 王伟 (id=3)
 *   pmUser          = 李娜 (id=4)     opsOwner      = 张强 (id=5)
 *   gateDecidedBy   = alice (admin)
 */

INSERT INTO rainier_opportunity
  (id, customer_id, customer_name, title, note, amount, stage, status,
   commercial_owner_user_id, solution_owner_user_id, pm_user_id, ops_owner_user_id,
   product_id, project_id, gate_decided_by, stage_entered_at,
   create_by, create_time, update_by, update_time, del_flag)
VALUES
  (1, 4, '招商银行', '数据资产管理平台',  '初步线索，CTO 引荐', 500000000, 'LEAD',         'OPEN',
   2, NULL, NULL, NULL, 1, NULL, NULL, DATE_SUB(@now, INTERVAL 35 DAY), @actor, DATE_SUB(@now, INTERVAL 35 DAY), @actor, DATE_SUB(@now, INTERVAL 35 DAY), 0),

  (2, 5, '中信集团', '财务对账系统',      '财务部门主导，需技术调研', 800000000, 'OPPORTUNITY',  'OPEN',
   2, 3, NULL, NULL, 1, NULL, NULL, DATE_SUB(@now, INTERVAL 22 DAY), @actor, DATE_SUB(@now, INTERVAL 30 DAY), @actor, DATE_SUB(@now, INTERVAL 22 DAY), 0),

  (3, 6, '远大科技', '智能客服改造',      'POC 测试 4 周', 300000000, 'POC',         'OPEN',
   2, 3, 4, NULL, 1, NULL, NULL, DATE_SUB(@now, INTERVAL 12 DAY), @actor, DATE_SUB(@now, INTERVAL 28 DAY), @actor, DATE_SUB(@now, INTERVAL 12 DAY), 0),

  (4, 7, '华峰制造', 'MES 生产管理系统',  '已提交标书，等待评审', 1200000000, 'BIDDING',     'OPEN',
   2, 3, 4, NULL, 1, NULL, NULL, DATE_SUB(@now, INTERVAL 8 DAY), @actor, DATE_SUB(@now, INTERVAL 40 DAY), @actor, DATE_SUB(@now, INTERVAL 8 DAY), 0),

  (5, 8, '蓝海物流', '仓配一体化平台',    '合同条款谈判中', 900000000, 'CONTRACT',     'OPEN',
   2, 3, 4, NULL, 1, NULL, NULL, DATE_SUB(@now, INTERVAL 5 DAY), @actor, DATE_SUB(@now, INTERVAL 50 DAY), @actor, DATE_SUB(@now, INTERVAL 5 DAY), 0),

  (6, 9, '星辰医疗', 'HIS 升级',          '已立项，进入现场调研', 600000000, 'INITIATION', 'WON',
   2, 3, 4, NULL, 1, NULL, 'alice', DATE_SUB(@now, INTERVAL 25 DAY), @actor, DATE_SUB(@now, INTERVAL 60 DAY), @actor, DATE_SUB(@now, INTERVAL 25 DAY), 0),

  (7, 4, '招商银行', '反欺诈风控平台',    '现场调研中', 700000000, 'SURVEY',       'WON',
   2, 3, 4, NULL, 1, NULL, 'alice', DATE_SUB(@now, INTERVAL 18 DAY), @actor, DATE_SUB(@now, INTERVAL 55 DAY), @actor, DATE_SUB(@now, INTERVAL 18 DAY), 0),

  (8, 5, '中信集团', '跨境结算系统',      '已生成诉求与需求，准备转交付', 400000000, 'REQUIREMENT', 'WON',
   2, 3, 4, NULL, 1, NULL, 'alice', DATE_SUB(@now, INTERVAL 10 DAY), @actor, DATE_SUB(@now, INTERVAL 70 DAY), @actor, DATE_SUB(@now, INTERVAL 10 DAY), 0),

  (9, 6, '远大科技', '知识图谱平台',      '实施中，已挂载交付项目', 1000000000, 'DELIVERY',    'WON',
   2, 3, 4, NULL, 1, NULL, 'alice', DATE_SUB(@now, INTERVAL 6 DAY), @actor, DATE_SUB(@now, INTERVAL 80 DAY), @actor, DATE_SUB(@now, INTERVAL 6 DAY), 0),

  (10, 7, '华峰制造', '智慧工厂一期',     '已验收，已转运营', 1500000000, 'ACCEPTANCE', 'WON',
   2, 3, 4, 5, 1, NULL, 'alice', DATE_SUB(@now, INTERVAL 3 DAY), @actor, DATE_SUB(@now, INTERVAL 90 DAY), @actor, DATE_SUB(@now, INTERVAL 3 DAY), 0);

ALTER TABLE rainier_opportunity AUTO_INCREMENT = 100;

/* ──────────────────────────────── opportunity_artifacts (per stage) ──────────────────────────── */

INSERT INTO rainier_opportunity_artifact
  (opportunity_id, type, title, content, link, stage_from, decision, create_by, create_time, update_by, update_time, del_flag)
VALUES
  /* opp 3 (POC) */
  (3, 'POC_REPORT', 'POC 验证报告', '## 4 周 POC 总结\n\n- 准确率 91.2%\n- 平均响应 1.2s\n- 通过率 ✅', NULL, 'POC', 'PASS', @actor, @now, @actor, @now, 0),

  /* opp 4 (BIDDING) */
  (4, 'OPPORTUNITY_RESEARCH_REPORT', '商机调研报告', '## 客户痛点\n\nMES 升级，现有系统数据孤岛严重', NULL, 'OPPORTUNITY', 'PASS', @actor, @now, @actor, @now, 0),
  (4, 'DECISION_REVIEW',             '决策评审纪要', '## 决议\n\n- 进入投标\n- 报价区间 1200-1500 万', NULL, 'POC',         'PASS', @actor, @now, @actor, @now, 0),
  (4, 'BIDDING_PROPOSAL',            '投标方案 v1',  NULL, 'https://docs.example.com/bid/huafeng-mes-v1.pdf', 'BIDDING', NULL, @actor, @now, @actor, @now, 0),

  /* opp 5 (CONTRACT) — all the BIDDING gates plus contract */
  (5, 'OPPORTUNITY_RESEARCH_REPORT', '商机调研报告', '## 客户痛点\n\n仓配链路割裂，需要一体化', NULL, 'OPPORTUNITY', 'PASS', @actor, @now, @actor, @now, 0),
  (5, 'DECISION_REVIEW',             '决策评审纪要', '## 决议\n\n投标 + 中标', NULL, 'POC',         'PASS', @actor, @now, @actor, @now, 0),
  (5, 'BIDDING_PROPOSAL',            '中标方案',     NULL, 'https://docs.example.com/bid/lanhai-wms.pdf', 'BIDDING',  NULL, @actor, @now, @actor, @now, 0),
  (5, 'CONTRACT',                    '商务合同 v1',  NULL, 'https://docs.example.com/contract/lanhai-v1.pdf', 'CONTRACT', NULL, @actor, @now, @actor, @now, 0),

  /* opp 6 (INITIATION) — has contract + initiation memo */
  (6, 'OPPORTUNITY_RESEARCH_REPORT', '商机调研报告', '## HIS 升级背景', NULL, 'OPPORTUNITY', 'PASS', @actor, @now, @actor, @now, 0),
  (6, 'DECISION_REVIEW',             '决策评审纪要', '## 决议：开始投标', NULL, 'POC', 'PASS', @actor, @now, @actor, @now, 0),
  (6, 'BIDDING_PROPOSAL',            '中标方案',     NULL, 'https://docs.example.com/bid/xingchen-his.pdf', 'BIDDING', NULL, @actor, @now, @actor, @now, 0),
  (6, 'CONTRACT',                    '正式合同',     NULL, 'https://docs.example.com/contract/xingchen-his.pdf', 'CONTRACT', NULL, @actor, @now, @actor, @now, 0),
  (6, 'INITIATION_MEMO',             '立项备忘',     '## 立项备忘\n\n- 项目编号：P-HIS-2026\n- 立项金额：600 万\n- 周期：6 个月', NULL, 'INITIATION', NULL, @actor, @now, @actor, @now, 0),

  /* opp 7 (SURVEY) — opp 6 + survey report */
  (7, 'OPPORTUNITY_RESEARCH_REPORT', '商机调研报告', '## 反欺诈背景', NULL, 'OPPORTUNITY', 'PASS', @actor, @now, @actor, @now, 0),
  (7, 'DECISION_REVIEW',             '决策评审纪要', '## 决议：投标', NULL, 'POC', 'PASS', @actor, @now, @actor, @now, 0),
  (7, 'BIDDING_PROPOSAL',            '中标方案',     NULL, 'https://docs.example.com/bid/zhaoshang-fraud.pdf', 'BIDDING', NULL, @actor, @now, @actor, @now, 0),
  (7, 'CONTRACT',                    '正式合同',     NULL, 'https://docs.example.com/contract/zhaoshang-fraud.pdf', 'CONTRACT', NULL, @actor, @now, @actor, @now, 0),
  (7, 'INITIATION_MEMO',             '立项备忘',     '## 立项备忘\n\nP-FRAUD-2026', NULL, 'INITIATION', NULL, @actor, @now, @actor, @now, 0),
  (7, 'SURVEY_REPORT',               '现场调研报告', '## 现场访谈纪要\n\n- 风控部 / 数据部访谈\n- 现有数据源 8 个\n- 关键流程 3 条', NULL, 'SURVEY', NULL, @actor, @now, @actor, @now, 0),
  (7, 'SURVEY_ATTACHMENT',           '调研附件包',   NULL, 'https://docs.example.com/survey/zhaoshang-fraud.zip', 'SURVEY', NULL, @actor, @now, @actor, @now, 0),

  /* opp 8 (REQUIREMENT) — opp 7 set */
  (8, 'OPPORTUNITY_RESEARCH_REPORT', '商机调研报告', '## 跨境结算背景', NULL, 'OPPORTUNITY', 'PASS', @actor, @now, @actor, @now, 0),
  (8, 'DECISION_REVIEW',             '决策评审纪要', '## 决议：投标', NULL, 'POC', 'PASS', @actor, @now, @actor, @now, 0),
  (8, 'BIDDING_PROPOSAL',            '中标方案',     NULL, 'https://docs.example.com/bid/zhongxin-xb.pdf', 'BIDDING', NULL, @actor, @now, @actor, @now, 0),
  (8, 'CONTRACT',                    '正式合同',     NULL, 'https://docs.example.com/contract/zhongxin-xb.pdf', 'CONTRACT', NULL, @actor, @now, @actor, @now, 0),
  (8, 'INITIATION_MEMO',             '立项备忘',     '## 立项备忘\n\nP-XBORDER-2026', NULL, 'INITIATION', NULL, @actor, @now, @actor, @now, 0),
  (8, 'SURVEY_REPORT',               '现场调研报告', '## 现场访谈\n\n- 财务/合规/IT 三方', NULL, 'SURVEY', NULL, @actor, @now, @actor, @now, 0),

  /* opp 9 (DELIVERY) — opp 8 set */
  (9, 'OPPORTUNITY_RESEARCH_REPORT', '商机调研报告', '## 知识图谱背景', NULL, 'OPPORTUNITY', 'PASS', @actor, @now, @actor, @now, 0),
  (9, 'DECISION_REVIEW',             '决策评审纪要', '## 决议：投标', NULL, 'POC', 'PASS', @actor, @now, @actor, @now, 0),
  (9, 'BIDDING_PROPOSAL',            '中标方案',     NULL, 'https://docs.example.com/bid/yuanda-kg.pdf', 'BIDDING', NULL, @actor, @now, @actor, @now, 0),
  (9, 'CONTRACT',                    '正式合同',     NULL, 'https://docs.example.com/contract/yuanda-kg.pdf', 'CONTRACT', NULL, @actor, @now, @actor, @now, 0),
  (9, 'INITIATION_MEMO',             '立项备忘',     '## 立项备忘\n\nP-KG-2026', NULL, 'INITIATION', NULL, @actor, @now, @actor, @now, 0),
  (9, 'SURVEY_REPORT',               '现场调研报告', '## 现场访谈\n\n- 数据团队 / 算法团队', NULL, 'SURVEY', NULL, @actor, @now, @actor, @now, 0),

  /* opp 10 (ACCEPTANCE) — opp 9 set + acceptance report */
  (10, 'OPPORTUNITY_RESEARCH_REPORT', '商机调研报告', '## 智慧工厂背景', NULL, 'OPPORTUNITY', 'PASS', @actor, @now, @actor, @now, 0),
  (10, 'DECISION_REVIEW',             '决策评审纪要', '## 决议：投标', NULL, 'POC', 'PASS', @actor, @now, @actor, @now, 0),
  (10, 'BIDDING_PROPOSAL',            '中标方案',     NULL, 'https://docs.example.com/bid/huafeng-iot.pdf', 'BIDDING', NULL, @actor, @now, @actor, @now, 0),
  (10, 'CONTRACT',                    '正式合同',     NULL, 'https://docs.example.com/contract/huafeng-iot.pdf', 'CONTRACT', NULL, @actor, @now, @actor, @now, 0),
  (10, 'INITIATION_MEMO',             '立项备忘',     '## 立项备忘\n\nP-FACTORY-2026', NULL, 'INITIATION', NULL, @actor, @now, @actor, @now, 0),
  (10, 'SURVEY_REPORT',               '现场调研报告', '## 工厂车间访谈', NULL, 'SURVEY', NULL, @actor, @now, @actor, @now, 0),
  (10, 'DELIVERY_ACCEPTANCE_REPORT',  '甲方验收报告', '## 验收报告\n\n- 功能验收：100%\n- 性能验收：通过\n- 客户签字：✅', NULL, 'DELIVERY', NULL, @actor, @now, @actor, @now, 0);

/* ─────────────────────────── projects (3 external-delivery + 1 internal) ─────────────────────── */

INSERT INTO rainier_project
  (id, code, name, description, owner_user_id, status, start_date, end_date, enabled, project_type, organization_id,
   create_by, create_time, update_by, update_time, del_flag)
VALUES
  (1, 'ED-1', 'HIS 升级实施',    '星辰医疗 HIS 升级 — 6 月期 (来源商机 #6)',
        4, 'ACTIVE',  DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 150 DAY), 1, 'EXTERNAL_DELIVERY', 2, @actor, @now, @actor, @now, 0),
  (2, 'ED-2', '反欺诈风控实施',  '招商银行风控平台 (来源商机 #7)',
        4, 'ACTIVE',  DATE_SUB(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 120 DAY), 1, 'EXTERNAL_DELIVERY', 2, @actor, @now, @actor, @now, 0),
  (3, 'ED-3', '知识图谱实施',    '远大科技 KG 平台 (来源商机 #9)',
        4, 'ACTIVE',  DATE_SUB(CURDATE(), INTERVAL 5 DAY),  DATE_ADD(CURDATE(), INTERVAL 90 DAY),  1, 'EXTERNAL_DELIVERY', 2, @actor, @now, @actor, @now, 0),
  (4, 'CF-4', '支付平台 2026H2', '内部主业 - 支付平台下半年迭代', 3, 'ACTIVE',  DATE_SUB(CURDATE(), INTERVAL 30 DAY), DATE_ADD(CURDATE(), INTERVAL 90 DAY),  1, 'CORE_FEATURE', 2, @actor, @now, @actor, @now, 0),
  (5, 'CT-5', '基础设施升级',    '内部主业 - 技术债清理 + 监控',     3, 'PLANNING', NULL, NULL, 1, 'CORE_TECH',    2, @actor, @now, @actor, @now, 0),
  (6, 'CAS-6', '演示沙盒',        '轻量项目，POC / Hack',              2, 'PLANNING', NULL, NULL, 1, 'CASUAL',       2, @actor, @now, @actor, @now, 0);

ALTER TABLE rainier_project AUTO_INCREMENT = 100;

/* Link external-delivery projects back into the WON opportunities */
UPDATE rainier_opportunity SET project_id = 1 WHERE id = 6;
UPDATE rainier_opportunity SET project_id = 2 WHERE id = 7;
UPDATE rainier_opportunity SET project_id = 3 WHERE id = 9;

/* ─────────────────────────── v0.0.64 — org PMOs + project pmo/team + members ─────────────────── */

/* organization_pmo: root 招联金融 (id=1) → alice(1); 研发中心 (id=2) → 黎立(2). 子组织无 own
 * → 演示「继承」效果（采购研发团队 effective-pmos = 黎立(own from 研发中心) + alice(继承 from 招联金融)). */
INSERT INTO rainier_organization_pmo (organization_id, user_id, create_by, create_time, update_by, update_time, del_flag) VALUES
  (1, 1, @actor, @now, @actor, @now, 0),
  (2, 2, @actor, @now, @actor, @now, 0);

/* projects: set pmo_user_id (default = effective-pmos 首条, 但这里手动指明以确保 demo 数据明确) */
UPDATE rainier_project SET pmo_user_id = 2 WHERE id IN (1, 2, 3, 4);  /* ED-1/2/3, CF-4 → 黎立 */
UPDATE rainier_project SET pmo_user_id = 1 WHERE id IN (5, 6);          /* CT-5, CAS-6 → alice */

/* project_member: 每 EXTERNAL_DELIVERY project 加 3-4 成员（不同 role），内部项目加 1-2 个. */
INSERT INTO rainier_project_member
  (project_id, user_id, role, joined_at, joined_by, create_by, create_time, update_by, update_time, del_flag)
VALUES
  /* ED-1 HIS 升级 (owner=4 lina, pmo=2 黎立) */
  (1, 3, 'PD',  DATE_SUB(@now, INTERVAL 18 DAY), 'lina', @actor, DATE_SUB(@now, INTERVAL 18 DAY), @actor, DATE_SUB(@now, INTERVAL 18 DAY), 0),
  (1, 5, 'DEV', DATE_SUB(@now, INTERVAL 17 DAY), 'lina', @actor, DATE_SUB(@now, INTERVAL 17 DAY), @actor, DATE_SUB(@now, INTERVAL 17 DAY), 0),
  (1, 6, 'DEV', DATE_SUB(@now, INTERVAL 15 DAY), 'lina', @actor, DATE_SUB(@now, INTERVAL 15 DAY), @actor, DATE_SUB(@now, INTERVAL 15 DAY), 0),
  (1, 7, 'QA',  DATE_SUB(@now, INTERVAL 10 DAY), 'lina', @actor, DATE_SUB(@now, INTERVAL 10 DAY), @actor, DATE_SUB(@now, INTERVAL 10 DAY), 0),

  /* ED-2 反欺诈风控 (owner=4 lina, pmo=2 黎立) */
  (2, 3, 'PD',     DATE_SUB(@now, INTERVAL 13 DAY), 'lina', @actor, DATE_SUB(@now, INTERVAL 13 DAY), @actor, DATE_SUB(@now, INTERVAL 13 DAY), 0),
  (2, 5, 'DEV',    DATE_SUB(@now, INTERVAL 12 DAY), 'lina', @actor, DATE_SUB(@now, INTERVAL 12 DAY), @actor, DATE_SUB(@now, INTERVAL 12 DAY), 0),
  (2, 7, 'DESIGN', DATE_SUB(@now, INTERVAL 10 DAY), 'lina', @actor, DATE_SUB(@now, INTERVAL 10 DAY), @actor, DATE_SUB(@now, INTERVAL 10 DAY), 0),

  /* ED-3 知识图谱 (owner=4 lina, pmo=2 黎立) */
  (3, 3, 'PD',  DATE_SUB(@now, INTERVAL 4 DAY), 'lina', @actor, DATE_SUB(@now, INTERVAL 4 DAY), @actor, DATE_SUB(@now, INTERVAL 4 DAY), 0),
  (3, 6, 'DEV', DATE_SUB(@now, INTERVAL 3 DAY), 'lina', @actor, DATE_SUB(@now, INTERVAL 3 DAY), @actor, DATE_SUB(@now, INTERVAL 3 DAY), 0),
  (3, 7, 'QA',  DATE_SUB(@now, INTERVAL 2 DAY), 'lina', @actor, DATE_SUB(@now, INTERVAL 2 DAY), @actor, DATE_SUB(@now, INTERVAL 2 DAY), 0),
  (3, 4, 'OTHER', DATE_SUB(@now, INTERVAL 1 DAY), 'liling', @actor, DATE_SUB(@now, INTERVAL 1 DAY), @actor, DATE_SUB(@now, INTERVAL 1 DAY), 0),  /* 故意试 owner=lina (id=4) 加成员 — service 会拒绝；先注释掉 */
  (3, 5, 'BIZ', DATE_SUB(@now, INTERVAL 5 DAY), 'liling', @actor, DATE_SUB(@now, INTERVAL 5 DAY), @actor, DATE_SUB(@now, INTERVAL 5 DAY), 0),

  /* CF-4 支付平台 2026H2 (owner=3 wangwei, pmo=2 黎立) */
  (4, 5, 'DEV', DATE_SUB(@now, INTERVAL 25 DAY), 'wangwei', @actor, DATE_SUB(@now, INTERVAL 25 DAY), @actor, DATE_SUB(@now, INTERVAL 25 DAY), 0),
  (4, 7, 'QA',  DATE_SUB(@now, INTERVAL 22 DAY), 'wangwei', @actor, DATE_SUB(@now, INTERVAL 22 DAY), @actor, DATE_SUB(@now, INTERVAL 22 DAY), 0),

  /* CT-5 基础设施 (owner=3 wangwei, pmo=1 alice) */
  (5, 6, 'OPS', DATE_SUB(@now, INTERVAL 1 DAY), 'wangwei', @actor, DATE_SUB(@now, INTERVAL 1 DAY), @actor, DATE_SUB(@now, INTERVAL 1 DAY), 0),

  /* CAS-6 演示沙盒 (owner=2 黎立, pmo=1 alice) */
  (6, 4, 'PD',  DATE_SUB(@now, INTERVAL 2 DAY), 'liling', @actor, DATE_SUB(@now, INTERVAL 2 DAY), @actor, DATE_SUB(@now, INTERVAL 2 DAY), 0);

/* DELETE the bogus row above (ED-3 加 owner=lina 自己, 演示 only): 实际后端不允许；这里 SQL bypass 了校验，
 * 但为了不让 UI 列表 UNION 出现重复，我们移除它。 */
DELETE FROM rainier_project_member WHERE project_id = 3 AND user_id = 4;

/* ─────────────────────────────────── milestones (4) ──────────────────────────────────────────── */

INSERT INTO rainier_milestone
  (project_id, code, name, description, target_date, actual_date, status, sort_order,
   create_by, create_time, update_by, update_time, del_flag)
VALUES
  (1, 'M-HIS-DESIGN',    '需求与设计', '完成需求 + 详设',     DATE_SUB(CURDATE(), INTERVAL 5 DAY),  DATE_SUB(CURDATE(), INTERVAL 4 DAY), 'REACHED', 1, @actor, @now, @actor, @now, 0),
  (1, 'M-HIS-DEV',       '开发完成',    '主功能开发完成',       DATE_ADD(CURDATE(), INTERVAL 60 DAY),  NULL,                                'PLANNED', 2, @actor, @now, @actor, @now, 0),
  (3, 'M-KG-INGEST',     '数据接入',    '完成 3 类源接入',     DATE_ADD(CURDATE(), INTERVAL 14 DAY), NULL,                                'PLANNED', 1, @actor, @now, @actor, @now, 0),
  (3, 'M-KG-QUERY',      '图查询上线', '基础图查询服务上线', DATE_ADD(CURDATE(), INTERVAL 60 DAY), NULL,                                'PLANNED', 2, @actor, @now, @actor, @now, 0);

/* ─────────────────────────────────── requirements (5) ────────────────────────────────────────── */

INSERT INTO rainier_requirement
  (id, code, title, description, owner_user_id, status, priority, complexity, project_id, opportunity_id, expected_date,
   create_by, create_time, update_by, update_time, del_flag)
VALUES
  (1, 'REQ-1', '跨境结算 — 多币种支持',  '## 描述\n\n支持 USD/EUR/HKD/JPY 四币种，支持实时汇率', 3, 'IN_ANALYSIS', 'HIGH',   'L',  NULL, 8, DATE_ADD(CURDATE(), INTERVAL 60 DAY), @actor, @now, @actor, @now, 0),
  (2, 'REQ-2', '跨境结算 — 合规筛查',    '## 描述\n\n反洗钱黑名单 + 制裁名单实时校验',         4, 'DRAFT',       'URGENT', 'XL', NULL, 8, DATE_ADD(CURDATE(), INTERVAL 70 DAY), @actor, @now, @actor, @now, 0),
  (3, 'REQ-3', '知识图谱 — 实体抽取',    '## 描述\n\n基于 LLM 的实体关系抽取',                  3, 'IN_PROGRESS', 'HIGH',   'L',  3,    9, DATE_ADD(CURDATE(), INTERVAL 30 DAY), @actor, @now, @actor, @now, 0),
  (4, 'REQ-4', '知识图谱 — 图查询 API',  '## 描述\n\n标准 Cypher / Gremlin 子集',              4, 'IN_PROGRESS', 'MEDIUM', 'M',  3,    9, DATE_ADD(CURDATE(), INTERVAL 45 DAY), @actor, @now, @actor, @now, 0),
  (5, 'REQ-5', '支付平台 — 风控规则引擎', '## 描述\n\n可视化规则编辑 + 灰度发布',               3, 'DELIVERED',   'HIGH',   'L',  4,    NULL, DATE_SUB(CURDATE(), INTERVAL 5 DAY),  @actor, @now, @actor, @now, 0);

ALTER TABLE rainier_requirement AUTO_INCREMENT = 100;

/* ─────────────────────────────────── sprints (4) ─────────────────────────────────────────────── */

INSERT INTO rainier_sprint
  (id, code, name, description, goal, status, requirement_id, product_id, owner_user_id, start_date, end_date,
   create_by, create_time, update_by, update_time, del_flag)
VALUES
  (1, 'SPR-KG-S1', 'KG Sprint 1',  '抽取算法基础工程',  '完成实体抽取 baseline + 1 个数据源 ingest', 'COMPLETED', 3, 1, 4, DATE_SUB(CURDATE(), INTERVAL 28 DAY), DATE_SUB(CURDATE(), INTERVAL 14 DAY), @actor, @now, @actor, @now, 0),
  (2, 'SPR-KG-S2', 'KG Sprint 2',  '抽取算法优化',       '准确率 ≥ 88% / 覆盖率 ≥ 80%',                'ACTIVE',    3, 1, 4, DATE_SUB(CURDATE(), INTERVAL 13 DAY), DATE_ADD(CURDATE(), INTERVAL 1 DAY),   @actor, @now, @actor, @now, 0),
  (3, 'SPR-KG-Q1', 'KG Query S1',  '图查询 API 一期',    '基础 CRUD + 节点边查询',                     'PLANNING',  4, 1, 4, DATE_ADD(CURDATE(), INTERVAL 2 DAY),  DATE_ADD(CURDATE(), INTERVAL 18 DAY), @actor, @now, @actor, @now, 0),
  (4, 'SPR-PAY-R1','Pay Risk S1',  '规则引擎核心',       '规则 DSL + 解析器',                          'COMPLETED', 5, 1, 3, DATE_SUB(CURDATE(), INTERVAL 35 DAY), DATE_SUB(CURDATE(), INTERVAL 21 DAY), @actor, @now, @actor, @now, 0);

ALTER TABLE rainier_sprint AUTO_INCREMENT = 100;

/* sprint - feature link (just link KG S2 + Pay R1 to existing features so the link panel has data) */
INSERT INTO rainier_sprint_feature (sprint_id, feature_id, create_by, create_time, update_by, update_time, del_flag) VALUES
  (4, 1, @actor, @now, @actor, @now, 0),
  (4, 2, @actor, @now, @actor, @now, 0);

/* ───────────────────────────────────── stories (8) ───────────────────────────────────────────── */

INSERT INTO rainier_story
  (id, code, title, description, acceptance_criteria, status, priority, complexity, sprint_id, owner_user_id, project_id,
   create_by, create_time, update_by, update_time, del_flag)
VALUES
  (1, 'STR-1', '实体抽取 — 名词识别', '中文 NER 子任务',     '准确率 ≥ 90% on test set', 'DONE',       'HIGH',   'M', 1, 4, 3, @actor, @now, @actor, @now, 0),
  (2, 'STR-2', '实体抽取 — 关系识别', '名词对关系标注',     '召回率 ≥ 75%',                 'DONE',       'HIGH',   'M', 1, 4, 3, @actor, @now, @actor, @now, 0),
  (3, 'STR-3', '实体抽取 — 性能优化', '吞吐量 5x',           '吞吐量翻 5 倍',                'IN_PROGRESS','HIGH',   'L', 2, 4, 3, @actor, @now, @actor, @now, 0),
  (4, 'STR-4', '实体抽取 — 准确率调优','优化 LLM prompt',    '准确率 ≥ 88%',                 'IN_PROGRESS','MEDIUM', 'M', 2, 4, 3, @actor, @now, @actor, @now, 0),
  (5, 'STR-5', '图查询 — Cypher 子集', '基础 MATCH/WHERE',  '5 个核心 query 类型支持',     'READY',      'HIGH',   'L', 3, 4, 3, @actor, @now, @actor, @now, 0),
  (6, 'STR-6', '图查询 — REST 包装',  'REST API 包装',       'OpenAPI 文档完整',             'READY',      'MEDIUM', 'M', 3, 4, 3, @actor, @now, @actor, @now, 0),
  (7, 'STR-7', '规则 DSL',             'JSON-based DSL',     'Schema 校验通过',              'DONE',       'HIGH',   'M', 4, 3, 4, @actor, @now, @actor, @now, 0),
  (8, 'STR-8', '规则解析器',           'AST → IR',           '所有用例通过',                 'DONE',       'HIGH',   'L', 4, 3, 4, @actor, @now, @actor, @now, 0);

ALTER TABLE rainier_story AUTO_INCREMENT = 100;

/* ─────────────────────────────────── tasks (16) ──────────────────────────────────────────────── */

INSERT INTO rainier_task
  (id, code, title, description, status, priority, project_id, sprint_id, story_id, assignee_user_id, due_date,
   create_by, create_time, update_by, update_time, del_flag)
VALUES
  (1,  'TSK-1',  'NER baseline 模型选型',   'BERT / RoBERTa 对比', 'DONE',        'HIGH',   3, 1, 1, 4, DATE_SUB(CURDATE(), INTERVAL 20 DAY), @actor, @now, @actor, @now, 0),
  (2,  'TSK-2',  '标注数据采购',             '5w 条标注数据',       'DONE',        'HIGH',   3, 1, 1, 5, DATE_SUB(CURDATE(), INTERVAL 18 DAY), @actor, @now, @actor, @now, 0),
  (3,  'TSK-3',  '关系标注模型训练',         'BERT-CRF',           'DONE',        'HIGH',   3, 1, 2, 4, DATE_SUB(CURDATE(), INTERVAL 16 DAY), @actor, @now, @actor, @now, 0),
  (4,  'TSK-4',  '关系评测脚本',             'F1 计算脚本',         'DONE',        'MEDIUM', 3, 1, 2, 6, DATE_SUB(CURDATE(), INTERVAL 15 DAY), @actor, @now, @actor, @now, 0),
  (5,  'TSK-5',  '推理引擎优化',             'ONNX + batching',     'IN_PROGRESS', 'HIGH',   3, 2, 3, 4, DATE_ADD(CURDATE(), INTERVAL 2 DAY),  @actor, @now, @actor, @now, 0),
  (6,  'TSK-6',  '吞吐压测脚本',             'wrk + 报告',         'TODO',        'MEDIUM', 3, 2, 3, 5, DATE_ADD(CURDATE(), INTERVAL 4 DAY),  @actor, @now, @actor, @now, 0),
  (7,  'TSK-7',  'Prompt 调优',              '5 轮 prompt 迭代',    'IN_PROGRESS', 'MEDIUM', 3, 2, 4, 4, DATE_ADD(CURDATE(), INTERVAL 5 DAY),  @actor, @now, @actor, @now, 0),
  (8,  'TSK-8',  '准确率回归测试',           '回归用例 200 条',     'BLOCKED',     'HIGH',   3, 2, 4, 6, DATE_ADD(CURDATE(), INTERVAL 8 DAY),  @actor, @now, @actor, @now, 0),
  (9,  'TSK-9',  'Cypher 解析器',            'MATCH / WHERE 子集', 'TODO',        'HIGH',   3, 3, 5, 4, DATE_ADD(CURDATE(), INTERVAL 12 DAY), @actor, @now, @actor, @now, 0),
  (10, 'TSK-10', 'Cypher 执行器',            '简单 BFS 执行',       'TODO',        'HIGH',   3, 3, 5, 4, DATE_ADD(CURDATE(), INTERVAL 14 DAY), @actor, @now, @actor, @now, 0),
  (11, 'TSK-11', 'REST API 包装',            'Spring controller',  'TODO',        'MEDIUM', 3, 3, 6, 6, DATE_ADD(CURDATE(), INTERVAL 14 DAY), @actor, @now, @actor, @now, 0),
  (12, 'TSK-12', 'OpenAPI 文档',             '生成 spec',           'TODO',        'LOW',    3, 3, 6, 7, DATE_ADD(CURDATE(), INTERVAL 16 DAY), @actor, @now, @actor, @now, 0),
  (13, 'TSK-13', '规则 DSL Schema',          'JSON-Schema',         'DONE',        'HIGH',   4, 4, 7, 3, DATE_SUB(CURDATE(), INTERVAL 25 DAY), @actor, @now, @actor, @now, 0),
  (14, 'TSK-14', '规则 DSL 用例',            '50 条规则用例',       'DONE',        'MEDIUM', 4, 4, 7, 7, DATE_SUB(CURDATE(), INTERVAL 24 DAY), @actor, @now, @actor, @now, 0),
  (15, 'TSK-15', '解析器 AST',               '基础 AST 节点',       'DONE',        'HIGH',   4, 4, 8, 3, DATE_SUB(CURDATE(), INTERVAL 22 DAY), @actor, @now, @actor, @now, 0),
  (16, 'TSK-16', '解析器 IR',                'AST → IR 转换',       'DONE',        'HIGH',   4, 4, 8, 6, DATE_SUB(CURDATE(), INTERVAL 21 DAY), @actor, @now, @actor, @now, 0);

ALTER TABLE rainier_task AUTO_INCREMENT = 100;

/* ─────────────────────────────────── demands (6) ─────────────────────────────────────────────── */

INSERT INTO rainier_demand
  (id, title, description, submitter_user_id, status, priority, source, opportunity_id,
   create_by, create_time, update_by, update_time, del_flag)
VALUES
  (1, '希望支持微信小程序 H5 嵌入', '客户 A 反馈：希望在小程序内嵌入余额查询', 6, 'PENDING',   'HIGH',   'WECHAT', NULL, @actor, DATE_SUB(@now, INTERVAL 5 DAY),  @actor, DATE_SUB(@now, INTERVAL 5 DAY),  0),
  (2, '提现 T+0 到账',                'VIP 客户希望提现实时到账',                7, 'PENDING',   'EMAIL',  'EMAIL', NULL, @actor, DATE_SUB(@now, INTERVAL 8 DAY),  @actor, DATE_SUB(@now, INTERVAL 8 DAY),  0),
  (3, '余额预警推送',                  '余额低于阈值短信通知',                     4, 'IN_REVIEW', 'MEDIUM', 'WEB',  NULL, @actor, DATE_SUB(@now, INTERVAL 10 DAY), @actor, DATE_SUB(@now, INTERVAL 6 DAY), 0),
  (4, '跨境结算 - 增加日元支持',       '中信集团提：跨境结算支持 JPY',           5, 'CONVERTED', 'HIGH',   'DINGTALK', 8, @actor, DATE_SUB(@now, INTERVAL 30 DAY), @actor, DATE_SUB(@now, INTERVAL 10 DAY), 0),
  (5, '跨境结算 - 反洗钱筛查',         '中信集团提：反洗钱黑名单实时校验',       5, 'CONVERTED', 'URGENT', 'DINGTALK', 8, @actor, DATE_SUB(@now, INTERVAL 28 DAY), @actor, DATE_SUB(@now, INTERVAL 9 DAY),  0),
  (6, '反欺诈规则可视化',              '规则编辑流程过于复杂，想可视化',         4, 'DONE',      'MEDIUM', 'OTHER',  NULL, @actor, DATE_SUB(@now, INTERVAL 60 DAY), @actor, DATE_SUB(@now, INTERVAL 20 DAY), 0);

ALTER TABLE rainier_demand AUTO_INCREMENT = 100;

/* dr_link: 跨境结算诉求 → 跨境结算需求 */
INSERT INTO rainier_demand_requirement (demand_id, requirement_id, link_type, create_by, create_time, update_by, update_time, del_flag) VALUES
  (4, 1, 'DERIVED', @actor, @now, @actor, @now, 0),
  (5, 2, 'DERIVED', @actor, @now, @actor, @now, 0);

/* ─────────────────────────────── operations (3) + issues (5) ────────────────────────────────── */

INSERT INTO rainier_operation
  (id, customer_name, title, ops_owner_user_id, project_id, opportunity_id, stage, status,
   create_by, create_time, update_by, update_time, del_flag)
VALUES
  (1, '华峰制造', '智慧工厂一期 - 运维',  5, NULL, 10,   'MAINTENANCE', 'ACTIVE', @actor, DATE_SUB(@now, INTERVAL 3 DAY),  @actor, DATE_SUB(@now, INTERVAL 3 DAY),  0),
  (2, '蓝海物流', '仓配系统 - 日常运营', 5, NULL, NULL, 'OPERATING',   'ACTIVE', @actor, DATE_SUB(@now, INTERVAL 90 DAY), @actor, DATE_SUB(@now, INTERVAL 90 DAY), 0),
  (3, '远大科技', 'CRM 服务 - 续约期',   5, NULL, NULL, 'REPURCHASE',  'ACTIVE', @actor, DATE_SUB(@now, INTERVAL 180 DAY), @actor, DATE_SUB(@now, INTERVAL 180 DAY), 0);

ALTER TABLE rainier_operation AUTO_INCREMENT = 100;

INSERT INTO rainier_operation_issue
  (operation_id, title, description, severity, status, reporter_user_id, assignee_user_id, close_reason,
   create_by, create_time, update_by, update_time, del_flag)
VALUES
  (1, '生产线 #2 上传数据延迟',  '一线工长反馈：MES 上报偶发延迟 30s+', 'HIGH',   'OPEN',         5, 4,    NULL,             @actor, DATE_SUB(@now, INTERVAL 2 DAY), @actor, DATE_SUB(@now, INTERVAL 2 DAY), 0),
  (1, '物料编码缺失映射',         '半成品物料新编码未同步到 MES',         'MEDIUM', 'IN_PROGRESS',  5, 4,    NULL,             @actor, DATE_SUB(@now, INTERVAL 1 DAY), @actor, DATE_SUB(@now, INTERVAL 1 DAY), 0),
  (2, '日报推送失败',             '凌晨 2:00 日报邮件未发送',             'LOW',    'RESOLVED',     5, 5,    '调度任务被误删，已恢复',           @actor, DATE_SUB(@now, INTERVAL 7 DAY),  @actor, DATE_SUB(@now, INTERVAL 5 DAY),  0),
  (2, '导入工具偶发 OOM',         '大批量导入超 5w 行内存溢出',           'HIGH',   'CLOSED',       5, 6,    '已切换流式导入，监控正常',          @actor, DATE_SUB(@now, INTERVAL 20 DAY), @actor, DATE_SUB(@now, INTERVAL 10 DAY), 0),
  (3, '续约报价节奏沟通',         '商务部催 BOM 报价更新',                'LOW',    'OPEN',         5, 2,    NULL,             @actor, DATE_SUB(@now, INTERVAL 3 DAY),  @actor, DATE_SUB(@now, INTERVAL 3 DAY),  0);

SET FOREIGN_KEY_CHECKS = 1;

/* ─────────────────────────────────── verification ────────────────────────────────────────────── */

SELECT 'opportunity'         AS t, COUNT(*) AS c FROM rainier_opportunity         WHERE del_flag=0 UNION ALL
SELECT 'opportunity_artifact',   COUNT(*)      FROM rainier_opportunity_artifact WHERE del_flag=0 UNION ALL
SELECT 'operation',              COUNT(*)      FROM rainier_operation            WHERE del_flag=0 UNION ALL
SELECT 'operation_issue',        COUNT(*)      FROM rainier_operation_issue      WHERE del_flag=0 UNION ALL
SELECT 'project',                COUNT(*)      FROM rainier_project              WHERE del_flag=0 UNION ALL
SELECT 'project_member',         COUNT(*)      FROM rainier_project_member       WHERE del_flag=0 UNION ALL
SELECT 'organization_pmo',       COUNT(*)      FROM rainier_organization_pmo     WHERE del_flag=0 UNION ALL
SELECT 'milestone',              COUNT(*)      FROM rainier_milestone            WHERE del_flag=0 UNION ALL
SELECT 'requirement',            COUNT(*)      FROM rainier_requirement          WHERE del_flag=0 UNION ALL
SELECT 'sprint',                 COUNT(*)      FROM rainier_sprint               WHERE del_flag=0 UNION ALL
SELECT 'story',                  COUNT(*)      FROM rainier_story                WHERE del_flag=0 UNION ALL
SELECT 'task',                   COUNT(*)      FROM rainier_task                 WHERE del_flag=0 UNION ALL
SELECT 'feature',                COUNT(*)      FROM rainier_feature              WHERE del_flag=0 UNION ALL
SELECT 'demand',                 COUNT(*)      FROM rainier_demand               WHERE del_flag=0 UNION ALL
SELECT 'demand_requirement',     COUNT(*)      FROM rainier_demand_requirement   WHERE del_flag=0 UNION ALL
SELECT 'customer',               COUNT(*)      FROM rainier_customer             WHERE del_flag=0 UNION ALL
SELECT 'product',                COUNT(*)      FROM rainier_product              WHERE del_flag=0 UNION ALL
SELECT 'product_module',         COUNT(*)      FROM rainier_product_module       WHERE del_flag=0;
