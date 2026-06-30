# Test Report — Workbench Project Detail Links (I2, v0.0.115)

## RED

- `cd frontend && npm test -- --run src/pages/Workbench/WorkbenchPage.test.tsx`
- Result: failed because project row href was `/pm/projects`.

## GREEN

- Same command.
- Result: 1 file passed, 5 tests passed.

## Caveats

- None. This is a narrow front-end link fix.
