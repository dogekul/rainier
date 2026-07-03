# Capability: dev-runtime

## MODIFIED Requirements

### Requirement: 本地 Vite 代理支持 127.0.0.1 预览登录

Vite dev server 通过 `/api` 代理访问 backend 时 SHALL 不因浏览器 Origin 为本地临时端口而触发后端 CORS 拦截。

#### Scenario: 127.0.0.1 dev 端口登录可用

- **GIVEN** Rainier frontend dev server 运行在 `http://127.0.0.1:5174`
- **AND** backend 运行在 `http://localhost:8080`
- **WHEN** 浏览器从 `http://127.0.0.1:5174/login` 提交 `POST /api/auth/login`
- **THEN** Vite proxy SHALL 将该请求转发到 backend
- **AND** backend SHALL NOT 返回 `403 Invalid CORS request`
- **AND** 使用有效账号密码 SHALL 成功登录进入工作台
