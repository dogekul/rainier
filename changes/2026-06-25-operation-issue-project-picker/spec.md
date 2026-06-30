# Spec: Operation Issue Project Picker

## Scenario 1: Convert Through Project Picker

Given an operation detail page has an open issue
When the user clicks `转工单`
Then the page loads projects with `listProjects({ size: 200 })`
And displays an inline project picker for that issue
And does not call `window.prompt`

When the user chooses a project and confirms
Then the page calls `convertOperationIssueToTask(issueId, projectId)`
And closes the picker after a successful conversion
And reloads the issue list.

## Scenario 2: Default Project

Given the operation has `projectId`
And that project exists in the loaded project list
When the picker opens
Then that project is selected by default.

## Scenario 3: No Projects

Given the project list is empty
When the picker opens
Then the confirm action is disabled
And the user sees an inline message instead of a prompt.
