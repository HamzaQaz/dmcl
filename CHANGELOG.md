# Changelog

## 0.2.0

- **Multi-platform.** DMCL now ships for Fabric, NeoForge, Forge and Paper from one codebase.
  - Fabric: Minecraft 1.21.1 to 1.21.11, plus 26.1, 26.1.1, 26.1.2, 26.2 and 26.3 (26.x needs Java 25).
  - NeoForge: Minecraft 1.21.1, 1.21.3 to 1.21.5, 1.21.8, 1.21.10, 1.21.11 and 26.1 to 26.3.
  - Forge: Minecraft 1.21.1, 1.21.3 to 1.21.11 and 26.1 to 26.3.
  - Paper plugin: one jar for Paper 1.21 and later (Java 21).
- **Fix:** `/link`, `/unlink` and `/dmcl status` were registered after the server built its command tree, so they did not exist until a `/reload`. They now register at startup.
- New DMCL icon and branding.
- Internal: shared startup code (`DmclBootstrap`), the Minecraft-facing adapter now uses Mojang names, and the package moved to `com.n1ghtlydev.dmcl`.

## 0.1.2

- Degrade gracefully when the Discord token is missing or the config is invalid; allow an inline token in `dmcl.toml`.

## 0.1.1

- Fix TOML config loading in the shaded jar.

## 0.1.0

- Initial release: Fabric 1.21.1 chat bridge with webhook-per-player, two-way mentions, replies, edits, attachments, server event embeds and account linking.
