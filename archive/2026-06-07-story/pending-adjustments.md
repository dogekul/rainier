# Pending Design Adjustments — v0.0.9-story

Long-range full_auto / Phase 4 BUILD captures.

## PA-1. RequirementsPage onCountChange memoization

- **Discovered**: M08 — first vitest run of `RequirementsPage.test.tsx` hung the worker
  (tinypool "Worker exited unexpectedly" after 1000s; the test process was stuck in an
  infinite render loop).
- **Root cause**: `RequirementsPage` passed an inline arrow `onCountChange={() => void
  list.refetch()}` to `StoryListPanel`. Each parent render created a fresh function
  identity → `StoryListPanel`'s `useCallback(refetch, [requirementId, onCountChange])`
  re-memoized → `useEffect([refetch, refreshKey])` re-ran → `listStories` → `setStories`
  + `onCountChange` → `list.refetch()` → parent re-render → loop.
- **Fix**: memoized `onStoryCountChange` in `RequirementsPage` with `useCallback`
  depending only on `list.refetch` (which is stable from `usePaginated`).
- **Spec impact**: none — Scenarios for storyCount stay-fresh behavior were not specified
  at this granularity; the stale-count-on-update behavior is implementation-level.
- **Status**: ✅ fixed and verified.
