# Capability: entity-milestone

> NEW capability from `changes/2026-06-12-milestone` (v0.0.17-milestone, 2026-06-12).
> Milestone = 项目内的**时点检查点**(与 Sprint 的时间盒区间、与放行门的质量决策均不同)。
> 挂 Project(projectId NN)。字段 code/name/description/targetDate(NN)/status(PLANNED/REACHED/MISSED)/
> actualDate/sortOrder。(projectId,code) service 级复合唯一。软删。status 自由改无状态机。
> 删除所属项目时里程碑级联软删(见 entity-project)。表 `rainier_milestone`。

## Requirements

### Requirement: 创建里程碑

后端 SHALL 通过 `POST /api/milestones` 接受 `projectId` + `code` + `name` + `targetDate`（必填），其余用默认值，持久化返回 201。

#### Scenario: 最小 payload 创建 + 默认 PLANNED / sortOrder 0

- **GIVEN** 项目 id=1 存在
- **WHEN** 客户端 `POST /api/milestones` body `{"projectId":1,"code":"M-1","name":"需求评审完成","targetDate":"2026-07-01"}`
- **THEN** 系统 SHALL 返回 201
- **AND** body.id SHALL 为正整数
- **AND** body SHALL 含 `projectId=1` / `code="M-1"` / `name="需求评审完成"` / `targetDate="2026-07-01"`
- **AND** body.status SHALL 为 `"PLANNED"` 且 body.sortOrder SHALL 为 `0`

#### Scenario: 显式 status / sortOrder / actualDate 创建

- **GIVEN** 项目 id=1 存在
- **WHEN** `POST /api/milestones` body 含 `"status":"REACHED","sortOrder":5,"actualDate":"2026-06-30"`
- **THEN** 系统 SHALL 返回 201
- **AND** body SHALL 含 `status="REACHED"` / `sortOrder=5` / `actualDate="2026-06-30"`

### Requirement: 创建校验

后端 SHALL 校验 projectId 存在、targetDate 必填、status 合法、(projectId,code) 复合唯一，违反则拒绝。

#### Scenario: projectId 不存在被拒

- **GIVEN** 数据库无 id=999999 的项目
- **WHEN** `POST /api/milestones` body 含 `"projectId":999999`
- **THEN** 系统 SHALL 返回 400
- **AND** body.message SHALL 含 `"project not found"`

#### Scenario: 缺 targetDate 被拒

- **GIVEN** 项目 id=1 存在
- **WHEN** `POST /api/milestones` body `{"projectId":1,"code":"M-2","name":"X"}`（无 targetDate）
- **THEN** 系统 SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 含 `"targetDate"`

#### Scenario: 缺 code 被拒

- **GIVEN** 项目 id=1 存在
- **WHEN** `POST /api/milestones` body `{"projectId":1,"name":"X","targetDate":"2026-07-01"}`（无 code）
- **THEN** 系统 SHALL 返回 400
- **AND** body.fieldErrors[*].field SHALL 含 `"code"`

#### Scenario: 非法 status 被拒

- **GIVEN** 项目 id=1 存在
- **WHEN** `POST /api/milestones` body 含 `"status":"DONE"`
- **THEN** 系统 SHALL 返回 400
- **AND** body.message SHALL 含 `"invalid status"`

#### Scenario: 同项目内 code 重复被拒

- **GIVEN** 项目 id=1 已有里程碑 `code="M-1"`
- **WHEN** 再 `POST /api/milestones` body 含 `"projectId":1,"code":"M-1"`
- **THEN** 系统 SHALL 返回 409
- **AND** body.message SHALL 含 `"code already exists"`

#### Scenario: 不同项目可用相同 code

- **GIVEN** 项目 id=1 已有里程碑 `code="M-1"`，项目 id=2 存在
- **WHEN** `POST /api/milestones` body 含 `"projectId":2,"code":"M-1","name":"X","targetDate":"2026-07-01"`
- **THEN** 系统 SHALL 返回 201

### Requirement: 查询里程碑（过滤 + 排序）

后端 SHALL 通过 `GET /api/milestones?projectId=&status=&page=&size=` 返回 PageResponse，默认按 sortOrder 升序、再 createTime 降序。

#### Scenario: 按 projectId 过滤并按 sortOrder 升序

- **GIVEN** 项目 id=1 有里程碑 A(sortOrder=2) 与 B(sortOrder=1)，项目 id=2 有里程碑 C
- **WHEN** `GET /api/milestones?projectId=1`
- **THEN** body.total SHALL 为 2
- **AND** body.content 全部 `projectId=1`（不含 C）
- **AND** body.content[0].sortOrder SHALL ≤ body.content[1].sortOrder（B 在 A 前）

#### Scenario: 按 status 过滤

- **GIVEN** 项目 id=1 有 2 个 PLANNED + 1 个 REACHED 里程碑
- **WHEN** `GET /api/milestones?projectId=1&status=PLANNED`
- **THEN** body.total SHALL 为 2
- **AND** body.content 全部 `status="PLANNED"`

### Requirement: 更新里程碑（自由改，无状态机）

后端 SHALL 通过 `PUT /api/milestones/{id}` 修改 code（重检复合唯一）/ name / description / targetDate / status / actualDate / sortOrder；projectId 不可改；status 非法则 400。

#### Scenario: 标记达成（PLANNED → REACHED + actualDate）

- **GIVEN** 里程碑 id=1 当前 `status="PLANNED"`，actualDate 为空
- **WHEN** `PUT /api/milestones/1` body 含 `"status":"REACHED","actualDate":"2026-07-02"`（+必填 code/name/targetDate）
- **THEN** 系统 SHALL 返回 200
- **AND** body SHALL 含 `status="REACHED"` / `actualDate="2026-07-02"`

#### Scenario: 调整 sortOrder 与 targetDate

- **GIVEN** 里程碑 id=1 当前 sortOrder=0
- **WHEN** `PUT /api/milestones/1` body 含 `"sortOrder":9,"targetDate":"2026-08-01"`
- **THEN** 系统 SHALL 返回 200
- **AND** body SHALL 含 `sortOrder=9` / `targetDate="2026-08-01"`

#### Scenario: 更新非法 status 被拒

- **GIVEN** 里程碑 id=1 存在
- **WHEN** `PUT /api/milestones/1` body 含 `"status":"XXX"`
- **THEN** 系统 SHALL 返回 400
- **AND** body.message SHALL 含 `"invalid status"`

### Requirement: 软删里程碑

后端 SHALL 通过 `DELETE /api/milestones/{id}` 标记 `del_flag=1`，后续读取该 id 返回 404。

#### Scenario: 软删成功

- **GIVEN** 里程碑 id=1 存在
- **WHEN** `DELETE /api/milestones/1`
- **THEN** 系统 SHALL 返回 204
- **AND** 后续 `GET /api/milestones/1` SHALL 返回 404
