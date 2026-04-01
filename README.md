# VelocityHotReloader

[![CI Lint](https://github.com/HauntedMC/VelocityHotReloader/actions/workflows/ci-lint.yml/badge.svg?branch=main)](https://github.com/HauntedMC/VelocityHotReloader/actions/workflows/ci-lint.yml)
[![CI Tests and Coverage](https://github.com/HauntedMC/VelocityHotReloader/actions/workflows/ci-tests-and-coverage.yml/badge.svg?branch=main)](https://github.com/HauntedMC/VelocityHotReloader/actions/workflows/ci-tests-and-coverage.yml)
[![Latest Release](https://img.shields.io/github/v/release/HauntedMC/VelocityHotReloader?sort=semver)](https://github.com/HauntedMC/VelocityHotReloader/releases/latest)
[![Java 21](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![License](https://img.shields.io/github/license/HauntedMC/VelocityHotReloader)](LICENSE)

Hot-load, unload, reload, and watch Velocity plugins without restarting your proxy.

## Quick Start

1. Place `VelocityHotReloader.jar` in your Velocity `plugins/` directory.
2. Start the proxy once to initialize plugin files.
3. Use `/vhr help` (alias for `/velocityhotreloader help`) to verify command registration.
4. Grant the permission nodes you want operators to use.

## Requirements

- Java 21
- Velocity 3.5.x

## Core Commands

- `/vhr reload`: reload VelocityHotReloader resources.
- `/vhr restart [--force|-f]`: restart VelocityHotReloader.
- `/vhr loadplugin <jarFiles...>`: load one or more plugin jars from the proxy `plugins/` directory.
- `/vhr unloadplugin <plugins...> [--force|-f]`: disable + unload plugins.
- `/vhr reloadplugin <plugins...> [--force|-f]`: reload plugins.
- `/vhr watchplugin <plugins...> [--force|-f]`: watch plugin jar changes and auto-reload on update.
- `/vhr unwatchplugin <plugin>`: stop watching a plugin.
- `/vhr plugininfo <plugin>`: show plugin metadata.
- `/vhr commandinfo <command>`: show command ownership metadata.
- `/vhr plugins [--version|-v]`: list loaded plugins, optionally with versions.

## Build From Source

```bash
./gradlew clean build
```

Output jar: `build/libs/VelocityHotReloader-<version>.jar`

## Version Bump Workflow

Use the helper script to bump semver, commit, and tag:

```bash
scripts/bump-version.sh patch
scripts/bump-version.sh minor --push
```

Options:

- `major|minor|patch`: required bump type
- `--push`: push branch + tag after creating them
- `--remote <name>`: push/check against a remote (default: `origin`)

## Learn More

- [Configuration Guide](docs/CONFIGURATION.md)
- [Documentation Index](docs/README.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Development Notes](docs/DEVELOPMENT.md)
- [Testing and Quality](docs/TESTING.md)
- [Contributing](CONTRIBUTING.md)

## Community

- [Support](SUPPORT.md)
- [Security Policy](SECURITY.md)
- [Code of Conduct](CODE_OF_CONDUCT.md)
