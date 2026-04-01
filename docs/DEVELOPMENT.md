# Development Notes

This page is for contributors who want a fast, reliable local workflow for VelocityHotReloader.

## Local Setup

```bash
./gradlew compileJava
```

Useful commands during development:

```bash
./gradlew test
./gradlew checkstyleMain checkstyleTest
./gradlew test jacocoTestReport
./gradlew build
```

## Recommended Workflow

1. Create a branch for one focused change.
2. Implement the change with tests in the same pass.
3. Run local validation (`test` and Checkstyle at minimum).
4. Update docs when behavior or operator workflow changes.
5. Open a PR with context, impact, and migration notes (if any).

## Engineering Guidelines

- Keep lifecycle operations deterministic; avoid side effects spread across unrelated classes.
- Prefer explicit error reporting over silent fallbacks for operator-facing behavior.
- Keep reflection usage isolated to wrapper classes and manager boundaries.
- Ensure tasks/listeners/watchers are cleaned up on disable or restart.
- Favor small, testable units over broad command handlers.

## Before You Open a PR

- Build succeeds locally.
- Relevant tests pass.
- New behavior is covered by tests.
- Checkstyle passes.
- Operator-visible errors are logged clearly.
