# v0.0.16-project-type 实现任务清单

## entity-project (后端)

- [ ] P01 `ProjectType` 常量类(CASUAL/FORMAL + ALL) + `Project.projectType` nullable 列 + getter/setter
- [ ] P02 `ProjectCreateRequest`/`ProjectUpdateRequest` 加 projectType(@Size≤16 可选);`ProjectDetail` 加字段 + `from` null→CASUAL 兜底
- [ ] P03 `ProjectService` create(默认CASUAL+校验)/update(present-validate-set,absent-preserve)/list(projectType filter) + `ProjectController` list param
- [ ] P04 `ProjectTypeBackfill` bootstrap runner
- [ ] P05 backend 测试(TC-PROJTYPE-001..010) + 同步既有 detail 字段集测试

## frontend-scaffold (前端)

- [ ] P06 `api/project.ts` 加 ProjectType 类型 + projectType 字段
- [ ] P07 `ProjectsPage` 类型 select + 类型列 + 类型过滤 + `ProjectsPage.test`(TC-FES-PROJTYPE-001..004)

## E2E

- [ ] P08 docker 重建 + 回填验证 + 转化链 + SHOW TABLES=18 + 既有数据不变(TC-E2E-PROJTYPE-001)
