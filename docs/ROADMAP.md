# Archiey development roadmap

This roadmap is directional, not a promise of release dates.

## Phase 1 — Establish a reliable baseline
- [ ] Upload and review the existing Android project source
- [ ] Verify reproducible debug builds
- [ ] Confirm file navigation and basic operations on device
- [ ] Audit permissions and error handling
- [ ] Add versioning and release notes

## Phase 2 — File operations and search
- [ ] Test copy, move, rename and delete on real storage
- [ ] Prevent accidental overwrites and partial data loss
- [ ] Test recursive search on large directory trees
- [ ] Add cancellation and clear operation feedback

## Phase 3 — Archive support
- [ ] Choose and document archive engine and dependencies
- [ ] Implement safe ZIP extraction
- [ ] Evaluate 7z and TAR support
- [ ] Handle path traversal, corrupt archives and storage exhaustion
- [ ] Add progress reporting and cancellation

## Phase 4 — Polish and compatibility
- [ ] Test Android version and device compatibility
- [ ] Profile memory, UI responsiveness and background I/O
- [ ] Accessibility and UX review
- [ ] Prepare first tagged release

## Long-term ideas
- A separately maintained Archiey Legacy edition for older Android devices, subject to feasibility
- Additional archive formats and advanced tools

**Principle:** Protect user data first; performance and appearance come next.
