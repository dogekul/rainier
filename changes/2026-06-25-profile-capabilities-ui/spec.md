# Spec — Profile Capabilities UI (I1)

## Scenario 1: show capability rows

- Given `/api/me/profile` returns `capabilities[]`
- When user opens `/profile`
- Then 能力标签 card renders each tag with name/category/level/source.

## Scenario 2: empty capabilities

- Given `/api/me/profile` returns `capabilities: []`
- When user opens `/profile`
- Then 能力标签 card remains visible with an empty state.
