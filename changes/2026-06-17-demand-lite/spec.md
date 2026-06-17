# Spec — demand-lite (frontend-scaffold MOD, v0.0.26)

## Requirement: a minimal, smart-defaulted submit form

### Scenario: form defaults
- **GIVEN** a logged-in user with an id
- **WHEN** /demand-submit renders
- **THEN** it SHALL show 主题 + 描述 + 优先级(=中) + a read-only 提交人 line, and NO submitter/status/source field

### Scenario: title required
- **GIVEN** 主题 is empty
- **WHEN** viewing the submit button
- **THEN** it SHALL be disabled

## Requirement: submission reuses createDemand with smart defaults

### Scenario: submit the essentials
- **GIVEN** 主题='登录慢', 优先级='高'
- **WHEN** the user submits
- **THEN** createDemand SHALL be called once with {title:'登录慢', submitterUserId:<store id>, priority:'HIGH'} (description omitted)

### Scenario: success confirmation then reset
- **GIVEN** createDemand resolves
- **WHEN** the response returns
- **THEN** a confirmation with the title + #id and a 再提一个 button SHALL show; clicking 再提一个 SHALL reset the form

### Scenario: guarded + forgiving
- **GIVEN** the store user id is null → submit SHALL be blocked with a message
- **AND** when createDemand rejects → an inline error SHALL show and the entered 主题 SHALL remain
