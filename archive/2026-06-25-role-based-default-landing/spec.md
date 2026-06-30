# Spec — Role-Based Default Landing (H6)

## Scenario 1: admin user lands on compliance dashboard
- Given alice 拥有 role(adminAccess=true)
- When GET /api/auth/me
- Then response.defaultLandingPath == "/sys/compliance"

## Scenario 2: PMO non-admin lands on company map
- Given user 拥有 role(code=PMO, adminAccess=false) 且无其他 admin role
- When GET /api/auth/me
- Then response.defaultLandingPath == "/pmo"

## Scenario 3: ARCHITECT role lands on architect workbench
- Given user 拥有 role(code=ARCHITECT) 且非 admin / 非 PMO
- When GET /api/auth/me
- Then response.defaultLandingPath == "/architect"

## Scenario 4: project owner lands on cockpit
- Given user 是某 project 的 ownerUserId 且无以上角色
- When GET /api/auth/me
- Then response.defaultLandingPath == "/pm/cockpit"

## Scenario 5: requirement owner lands on inbox
- Given user 是某 requirement 的 ownerUserId 且不是任何 project owner / 无以上角色
- When GET /api/auth/me
- Then response.defaultLandingPath == "/inbox"

## Scenario 6: default user lands on home
- Given user 没有任何上述特征
- When GET /api/auth/me
- Then response.defaultLandingPath == "/"

## Scenario 7: Login redirect honours defaultLandingPath
- Given me() 返回 defaultLandingPath="/pmo"
- When LoginPage 登录成功
- Then navigate("/pmo", { replace: true })

## OutOfScope
- 自定义首页
- 重定向循环防护
