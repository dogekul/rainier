# Spec: 下属面板入口

## Scenarios

### S1. HEAD 用户查询下属列表
- Given 用户 alice 是 org X 的 HEAD，org X 下有 bob、carol（`leftAt=null`），dave 已离职（`leftAt!=null`），alice 自己也算成员
- When alice 调用 `GET /api/me/subordinates`
- Then 返回 200，body 是数组，包含 bob、carol；不含 alice 本人；不含 dave
- And 每条记录含字段 `{id, loginName, displayName, primaryOrgName, contributionSummary: {weeklyTasksDone, totalTasks}}`

### S2. 非 HEAD 用户调用
- Given 用户 eve 不是任何 org 的 HEAD
- When eve 调用 `GET /api/me/subordinates`
- Then 返回 200，body 为空数组 `[]`

### S3. 未登录调用
- Given 未携带 token
- When 调用 `GET /api/me/subordinates`
- Then 返回 401

### S4. 前端 SubordinatesPage 渲染
- Given 后端返回 2 条下属
- When 渲染 `/me/subordinates`
- Then 表格 2 行；每行展示姓名 / 主组织 / 本周 done 数 / 任务总数 / 「查看档案」按钮

### S5. 点击查看档案跳转
- Given 表格已渲染
- When 点击 bob 行的「查看档案」
- Then 浏览器跳转至 `/users/{bobId}/profile`

### S6. 工作台 nav 显隐
- Given me() 返回 `leadTeams.length > 0`
- Then AppLayout 在「工作台」分组显示「我的下属」入口
- Given me() 返回 `leadTeams.length == 0`
- Then 该入口隐藏
