/**
 * v0.0.60 — centralized Chinese labels for enums.
 *
 * Many pages render raw enum codes (PLANNING / MEMBER / TECH) in <option> lists despite using
 * StatusChip with labels in their tables. This file collects the labels so option lists and chips
 * can share a single source of truth.
 *
 * Keep alphabetical inside each map; one map per enum type.
 */

import type { DemandStatus } from '../api/demand';
import type { OrganizationType } from '../api/organization';
import type { PositionCategory } from '../api/position';
import type { ProductStatus } from '../api/product';
import type { ProductModuleStatus } from '../api/productModule';
import type { ProjectStatus } from '../api/project';
import type { UserOrgRole } from '../api/userOrganization';

export const PROJECT_STATUS_LABELS: Record<ProjectStatus, string> = {
  PLANNING: '筹备',
  ACTIVE: '进行中',
  ON_HOLD: '暂停',
  DELIVERED: '已交付',
  ARCHIVED: '已归档',
};

export const PRODUCT_STATUS_LABELS: Record<ProductStatus, string> = {
  PLANNING: '筹备',
  ACTIVE: '进行中',
  SUNSET: '日落',
  ARCHIVED: '已归档',
};

export const PRODUCT_MODULE_STATUS_LABELS: Record<ProductModuleStatus, string> = {
  PLANNING: '筹备',
  ACTIVE: '进行中',
  DEPRECATED: '已废弃',
};

export const ORGANIZATION_TYPE_LABELS: Record<OrganizationType, string> = {
  COMPANY: '公司',
  DEPARTMENT: '部门',
  DOMAIN: '领域',
  TEAM: '小组',
  SUBGROUP: '子组',
};

export const POSITION_CATEGORY_LABELS: Record<PositionCategory, string> = {
  TECH: '技术',
  BIZ: '业务',
  PM: '产品',
  MGMT: '管理',
  OTHER: '其他',
};

export const USER_ORG_ROLE_LABELS: Record<UserOrgRole, string> = {
  MEMBER: '成员',
  HEAD: '负责人',
};

export const DEMAND_STATUS_LABELS: Record<DemandStatus, string> = {
  PENDING: '待评审',
  IN_REVIEW: '评审中',
  CONVERTED: '已转化',
  DONE: '已完成',
  CLOSED: '已关闭',
};

/** v0.0.60 — demand-requirement link types (NOT api/link.ts which is entity-link with different semantics). */
export const DR_LINK_TYPE_LABELS: Record<'DERIVED' | 'RELATED', string> = {
  DERIVED: '派生',
  RELATED: '关联',
};
