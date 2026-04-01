# VelocityHotReloader Docs

This folder is the practical guide for running, maintaining, and contributing to VelocityHotReloader.

## Start Here

If you run the plugin:

- [Configuration](CONFIGURATION.md): runtime files, message customization, and permission planning.
- [Architecture](ARCHITECTURE.md): how command handling, plugin lifecycle, and watcher internals fit together.

If you contribute code:

- [Development](DEVELOPMENT.md): local setup and day-to-day workflow.
- [Testing](TESTING.md): test strategy and local validation commands.
- [Contributing Guide](../CONTRIBUTING.md): pull request expectations.

## Release Notes

Releases are tag-driven.

Typical flow:

1. Ensure CI is green on your target branch.
2. Bump version and create a release tag with `scripts/bump-version.sh patch`.
3. Push branch + tag and monitor the release workflow.
