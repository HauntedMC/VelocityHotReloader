# Configuration Guide

This guide focuses on practical setup and safe operation of VelocityHotReloader in production-like environments.

## Runtime File Layout

VelocityHotReloader stores data in your Velocity plugin data folder:

- `plugins/velocityhotreloader/messages.json`: customizable MiniMessage-based text output.
- `plugins/velocityhotreloader/.pluginCommandsCache.json`: generated command ownership cache used by command metadata lookups.

The plugin currently exposes message configuration only. There is no separate `config.yml`.

## Message Customization

`messages.json` is copied from defaults on first startup, then merged/migrated on reload.

Recommended workflow:

1. Edit only keys you want to change.
2. Keep `config-version` intact.
3. Run `/vhr reload` after changes.
4. Validate command output from both console and player context.

## Permission Nodes

Assign permission nodes based on operator role:

- `velocityhotreloader.help`
- `velocityhotreloader.reload`
- `velocityhotreloader.restart`
- `velocityhotreloader.loadplugin`
- `velocityhotreloader.unloadplugin`
- `velocityhotreloader.reloadplugin`
- `velocityhotreloader.watchplugin`
- `velocityhotreloader.plugininfo`
- `velocityhotreloader.commandinfo`
- `velocityhotreloader.plugins`
- `velocityhotreloader.plugins.version`

## Operational Safety Notes

- `unloadplugin`, `reloadplugin`, and `watchplugin` enforce dependency safety by default.
- Use `--force` / `-f` only when you intentionally want to bypass dependency checks.
- VelocityHotReloader prevents self-reload through `reloadplugin`; use `/vhr restart` instead.

## Troubleshooting Tips

- If a plugin is reported as missing, ensure the jar exists in Velocity's `plugins/` directory.
- If command ownership data looks stale, restart VelocityHotReloader (`/vhr restart`).
- If watcher behavior looks noisy, confirm only one watch task is active per plugin and avoid bulk file sync bursts.
