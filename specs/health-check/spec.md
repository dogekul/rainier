# Capability: health-check

## ADDED Requirements

### Requirement: 后端健康检查接口

后端 SHALL 暴露 `GET /api/health`，返回当前进程的存活状态，供 Docker / 负载均衡器探测。

#### Scenario: 服务正常运行时返回 UP

- **GIVEN** Spring Boot 后端已成功启动并完成 context 加载
- **WHEN** 客户端发起 `GET /api/health`
- **THEN** 系统 SHALL 返回 HTTP 200
- **AND** 响应 Content-Type SHALL 为 `application/json`
- **AND** 响应 body SHALL 包含 `"status":"UP"`
