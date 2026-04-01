# Architecture Overview

VelocityHotReloader is a Velocity plugin focused on safe runtime plugin lifecycle operations: load, enable, disable, unload, reload, and watch-for-change reload.

## Design Goals

- Provide fast operational control without full proxy restarts.
- Keep plugin lifecycle operations explicit and auditable.
- Protect operators from accidental dependency breakage unless they intentionally force actions.
- Keep command and messaging behavior consistent for console and in-game audiences.

## Core Components

- `VelocityHotReloaded`: plugin bootstrap and lifecycle entrypoint.
- `CommandVHR`: Brigadier command tree for `/velocityhotreloader` and `/vhr`.
- `VelocityPluginManager`: lifecycle operations and dependency-aware ordering.
- `WatchManager` + `PluginWatcherTask`: asynchronous file watching and debounced reload execution.
- `VelocityPluginCommandManager`: persisted command-to-plugin mapping cache.
- `MessagesResource`: MiniMessage-backed message loading and rendering.

## Runtime Flow

Startup:

1. Plugin cache and message resources are initialized.
2. Command tree is registered once.
3. Manager instances are created for lifecycle, task scheduling, and watcher orchestration.

Command execution:

1. Permission checks happen at command-node level.
2. Input is parsed into plugin or jar targets.
3. Dependency checks run before unload/reload/watch operations.
4. Result objects aggregate outcomes and format user-facing responses.

Watcher execution:

1. A watch task tracks plugin jar create/modify/delete events.
2. Rapid file events are debounced into a single reload pass.
3. Reload results are reported back to initiating audiences.

## Integration Boundaries

- Lifecycle operations rely on Velocity internals through dedicated reflection wrappers.
- Plugin interaction is centralized in manager classes instead of being spread across command handlers.
- Messages and placeholders are separated from business logic through typed message keys.

## Why This Matters

For operators, this architecture reduces restart pressure and speeds troubleshooting.

For contributors, it keeps change boundaries clear: command parsing in command classes, lifecycle behavior in managers, and user-facing text in resources.
