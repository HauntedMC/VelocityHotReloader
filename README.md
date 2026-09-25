# VelocityHotReloader

[![CI](https://github.com/HauntedMC/VelocityHotReloader/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/HauntedMC/VelocityHotReloader/actions/workflows/ci.yml)
[![Latest Release](https://img.shields.io/github/v/release/HauntedMC/VelocityHotReloader?sort=semver)](https://github.com/HauntedMC/VelocityHotReloader/releases/latest)
[![Java 25](https://img.shields.io/badge/Java-25-007396?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![License](https://img.shields.io/github/license/HauntedMC/VelocityHotReloader)](LICENSE)

Hot-load, unload, reload, and watch Velocity plugins without restarting your proxy.

Run `./mvnw -B -ntp -Pplatform-acceptance verify` to boot a disposable Velocity instance and validate the command surface, dynamic
plugin lifecycle, watcher, and VHR self-restart against temporary plugins.

## Quick Start

1. Place `VelocityHotReloader-<version>.jar` in your Velocity `plugins/` directory.
2. Start the proxy once to initialize plugin files.
3. Use `/vhr help` (alias for `/velocityhotreloader help`) to verify command registration.
4. Grant the permission nodes you want operators to use.

## Requirements

- Java 25
- Velocity 4.2.x

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

Use Java 25. HauntedPlatform is resolved from GitHub Packages; set `PACKAGES_USER` and `PACKAGES_TOKEN` (with `read:packages`) for a fresh local Maven cache. The committed `.mvn/settings.xml` reads these variables.

```bash
./mvnw -B -ntp verify
```

Output jar: `target/VelocityHotReloader-<version>.jar`

## Release workflow

From clean `main`, run `./tools/release/update-version patch --pr` to open a reviewed version PR. CI tests the PR; after merge, GitHub Actions publishes the Maven package, verifies that it resolves, and creates the tag and downloadable release jar with a SHA-256 checksum. See [release tooling](tools/release/README.md).

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
