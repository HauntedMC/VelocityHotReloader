# Testing and Quality

Testing in this project is designed to catch regressions in plugin lifecycle behavior while keeping contributor workflow practical.

## Test Structure

Tests are organized under `src/test/java` and generally mirror production package boundaries:

- command tests for command parsing and handler behavior;
- manager tests for lifecycle sequencing and edge cases;
- utility/entity tests for deterministic supporting logic.

## Local Commands

Run tests:

```bash
./gradlew test
```

Run full quality checks:

```bash
./gradlew check
```

Run lint checks:

```bash
./gradlew checkstyleMain checkstyleTest
```

Generate local coverage report:

```bash
./gradlew test jacocoTestReport
```

## What to Test

When changing behavior, add or update tests close to that behavior:

- command changes: argument parsing, permission-sensitive execution paths, and user-visible outputs;
- lifecycle changes: dependency checks, load/unload ordering, and error propagation;
- watcher changes: file-event handling, debounce behavior, and recovery after deleted/recreated jars.

Focus on user-visible and regression-prone behavior.

## Test Quality Bar

Use these rules when adding or reviewing tests:

- prefer behavior assertions over "does not throw" assertions;
- validate happy path and failure path for lifecycle-sensitive changes;
- avoid static mocking when regular dependency boundaries are available;
- assert observable outcomes (results, side effects, emitted messages, interactions).

## Coverage Reports

After `jacocoTestReport`:

- HTML report: `build/reports/jacoco/test/html/index.html`
- XML report: `build/reports/jacoco/test/jacocoTestReport.xml`

## CI

CI validates Checkstyle, tests, and coverage report generation on pull requests and `main` branch updates.
