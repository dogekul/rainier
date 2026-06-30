# Operation Issue Project Picker

## Problem

`OperationDetailPage` converts an operation issue to a task through `window.prompt`, asking the user to type a raw project id.

That is fragile in normal use: users rarely know project ids, typo handling is poor, and the happy path feels unfinished even though the backend conversion endpoint is already available.

## Scope

- Replace the prompt-based project id input with an inline project picker.
- Load candidate projects with the existing `listProjects({ size: 200 })` API.
- Prefer the current operation's `projectId` when it is present in the list.
- Preserve the existing backend conversion endpoint and post-conversion issue reload.

## Out Of Scope

- Backend changes.
- Server-side project search or pagination inside the picker.
- A full modal/dialog design system pass for the CRM page.
