# VelocityHotReloader
Velocity Plugin Reloader

## Version bump workflow
Use the helper script to bump semver, commit, and tag:

```bash
scripts/bump-version.sh patch
scripts/bump-version.sh minor --push
```

Options:
- `major|minor|patch` required bump type
- `--push` push branch + tag after creating them
- `--remote <name>` push/check against a remote (default: `origin`)
