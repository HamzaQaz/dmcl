# DMCL — Minecraft ⇄ Discord Chat Bridge (Fabric 1.21.1)

**Status:** Draft for review
**Date:** 2026-05-03
**Owner:** n1ght
**Target:** Minecraft 1.21.1 — Fabric Loader, server-side mod
**Repo path:** `/home/night/projects/halfservers/westwardmc/dmcl`
**Deploy target:** auto-copy built jar to `/mnt/c/dev/mc-server/mods/`

---

## 1. Goal

A server-side Fabric mod that bridges Minecraft chat with Discord with feature
parity close to a native Discord channel: webhook-per-player presentation,
two-way mentions with real pings, replies, edits, attachments rendered as
clickable hover-text, embeds, custom emoji, reactions, and threads. Server
events (joins, leaves, deaths, advancements, lifecycle) also relay to Discord.

The existing sibling `discbot` (TypeScript) keeps owning panel/admin slash
commands; this mod owns the chat bridge and `/link`, `/unlink`, `/players`.

## 2. Non-Goals

- Client-side mod components — server-side only.
- Replacing the existing `discbot` for Pterodactyl panel commands.
- Bridging private (whisper / `/msg`) DMs to Discord DMs.
- Multi-server (cross-shard) chat aggregation.
- Telegram/Matrix/IRC support — the architecture leaves room (see §10) but
  none of it ships in v1.

## 3. Decisions Locked During Brainstorm

| Topic                        | Decision                                                                  |
| ---------------------------- | ------------------------------------------------------------------------- |
| Discord transport            | JDA 5.x directly inside the mod (no relay through `discbot`)              |
| Architecture                 | Hexagonal (ports & adapters), per global `CLAUDE.md`                      |
| Webhook-per-player           | Yes — each MC player appears as their own Discord user via webhook + head |
| Discord → MC mentions        | Real ping (sound + highlight) for linked + online players                 |
| MC → Discord mentions        | `@name` resolves through `LinkRepo` to `<@discordId>`                     |
| Replies / edits / deletes    | Yes — quoted preview, repost on edit, gravestone on delete                |
| Attachments                  | All kinds → typed clickable hover-text (`📷`, `🎬`, `🔊`, `📎`)             |
| Embeds / stickers / emoji    | Render best-effort as MC `Text`                                           |
| Reactions                    | Supported, default OFF (configurable)                                     |
| Threads                      | Routed by parent channel scope; thread name shown as prefix               |
| Channel mapping              | Fully configurable, any number of `[[channels]]` entries                  |
| Server events                | Joins, leaves, deaths, advancements, lifecycle                            |
| Account linking              | MC `/link` → 6-char code → Discord `/link <code>` (5-min TTL)             |
| Slash commands in this mod   | `/link`, `/unlink`, `/players` only                                       |
| Storage                      | SQLite at `<world>/dmcl/links.db` (configurable to game dir)              |
| Config format                | TOML at `<gameDir>/config/dmcl.toml`                                      |
| Token sourcing               | env var → `dmcl.env` → `dmcl.secrets.toml`, first match wins              |
| Build / Java                 | Gradle 8 + Loom 1.7, Java 21, JDA + sqlite-jdbc shaded                    |

## 4. Module Layout

```
dmcl/                                 (Fabric mod, single Gradle project)
└── src/main/java/com/westwardmc/dmcl/
    ├── core/                         (pure JVM; no MC/JDA/JDBC imports)
    │   ├── domain/
    │   │   ├── BridgeMessage.java
    │   │   ├── Author.java
    │   │   ├── Attachment.java
    │   │   ├── MentionToken.java
    │   │   ├── ReplyContext.java
    │   │   ├── LinkedAccount.java
    │   │   └── PendingLink.java
    │   ├── port/
    │   │   ├── DiscordPort.java
    │   │   ├── MinecraftPort.java
    │   │   ├── LinkRepo.java
    │   │   ├── ChannelMap.java
    │   │   └── Clock.java
    │   ├── translate/
    │   │   ├── DiscordToMc.java
    │   │   ├── McToDiscord.java
    │   │   ├── MentionResolver.java
    │   │   └── AttachmentRenderer.java
    │   └── orchestrator/
    │       ├── BridgeOrchestrator.java
    │       └── EventBus.java
    │
    ├── adapter/
    │   ├── jda/
    │   │   ├── JdaDiscordAdapter.java
    │   │   ├── JdaMessageListener.java
    │   │   ├── WebhookCache.java
    │   │   └── SlashCommandRouter.java
    │   ├── fabric/
    │   │   ├── FabricMinecraftAdapter.java
    │   │   ├── ChatEventHook.java
    │   │   ├── LifecycleHook.java
    │   │   ├── PlayerEventHook.java
    │   │   └── McLinkCommand.java
    │   ├── sqlite/
    │   │   └── SqliteLinkRepo.java
    │   ├── http/
    │   │   └── McHeadsAvatarService.java
    │   └── config/
    │       └── TomlConfigLoader.java
    │
    └── DmclMod.java                          (ModInitializer; composition root)
```

**Dependency rule:** `core/` imports only JDK + SLF4J. `adapter/*` may import
`core/` and its specific tech. `DmclMod.java` is the only place that wires
concrete adapter classes together.

## 5. Core Domain & Ports (Signatures)

```java
// ---- domain ----
record BridgeMessage(
    String id,
    Source source,
    Author author,
    String body,
    List<Attachment> attachments,
    Optional<ReplyContext> replyTo,
    Scope scope,
    Instant timestamp,
    boolean edited
) {}

record Author(
    Optional<UUID> mcUuid,
    Optional<Long> discordId,
    String displayName,
    String avatarUrl
) {}

record Attachment(Kind kind, URI url, String filename, long sizeBytes) {
    enum Kind { IMAGE, VIDEO, AUDIO, FILE }
}

record LinkedAccount(UUID mcUuid, long discordId, Instant linkedAt) {}
record PendingLink(UUID mcUuid, String code, Instant expiresAt) {}

enum Scope { GLOBAL, STAFF, DEATHS, ADVANCEMENTS, LIFECYCLE, CUSTOM }
enum Source { MINECRAFT, DISCORD }

// ---- ports ----
interface DiscordPort {
    PostedRef sendWebhook(Scope scope, Author asAuthor, String body,
                          List<Attachment> atts, Optional<ReplyContext> reply);
    void editWebhook(PostedRef ref, String newBody);
    void deleteWebhook(PostedRef ref);
    void postSystem(Scope scope, SystemEvent ev);
    void onInbound(Consumer<BridgeMessage> handler);
    void onReaction(Consumer<ReactionEvent> handler);
}

interface MinecraftPort {
    void broadcast(Scope scope, RenderedMcText text);
    void sendTo(UUID player, RenderedMcText text);
    Set<OnlinePlayer> getOnlinePlayers();
    String headUrlFor(UUID uuid);
    void onChat(Consumer<BridgeMessage> handler);
    void onLifecycle(Consumer<LifecycleEvent> handler);
    void onPlayerEvent(Consumer<PlayerEvent> handler);
}

interface LinkRepo {
    void savePending(PendingLink p);
    Optional<UUID> consumePending(String code);   // atomic
    void link(UUID mcUuid, long discordId);
    void unlinkByMc(UUID mcUuid);
    void unlinkByDiscord(long discordId);
    Optional<LinkedAccount> byMcUuid(UUID mcUuid);
    Optional<LinkedAccount> byDiscordId(long discordId);
    List<LinkedAccount> all();
}

interface ChannelMap {
    Optional<ChannelBinding> forScope(Scope s);
    List<Scope> all();
}
```

`RenderedMcText` is a thin wrapper around MC's `Text` so `core/` does not
import `net.minecraft.text.Text`. The Fabric adapter converts at the boundary.

## 6. Message Flows

### 6.1 MC → Discord

1. `ChatEventHook` fires on `ServerMessageEvents.CHAT_MESSAGE_SENT`. Captures
   `(player, text, scope=GLOBAL)` into a tiny record on the server thread.
2. Adapter wraps as `BridgeMessage(source=MINECRAFT, ...)` and submits to
   the orchestrator executor — the server thread returns immediately.
3. `McToDiscord.translate()` (orchestrator thread):
   - Strip `§` color codes.
   - Escape Discord MD specials (`*_~|>` and backticks).
   - Resolve `@name` tokens via `MentionResolver`:
     - linked + online → `<@discordId>` (real ping)
     - online but unlinked → bold name (no ping)
     - unknown → leave as plain text
   - URLs auto-link (Discord native).
4. `DiscordPort.sendWebhook(scope, author, translatedBody, [], empty)`.
5. `JdaDiscordAdapter` fetches/creates the channel webhook via
   `WebhookCache`, posts with `username=playerName`, `avatarUrl=mc-heads URL`.
6. `(mcMessageId → PostedRef)` stored in 5,000-entry in-memory LRU for future
   edit/delete correlation.

### 6.2 Discord → MC

1. `JdaMessageListener.onMessageReceived` on JDA gateway thread.
2. Loop guard: drop if `event.isWebhookMessage()` and the webhook ID matches
   one we own (per `WebhookCache`). Belt-and-suspenders dedupe by
   `(authorId, body, timestamp±2s)` LRU.
3. Build `BridgeMessage(source=DISCORD, ..., replyTo=...)`, submit to executor.
4. `DiscordToMc.translate()` produces `RenderedMcText`:
   - **Markdown:** recursive-descent parser for the subset MC can render:
     - bold → `§l`, italic → `§o`, underline → `§n`, strikethrough → `§m`
     - inline `code` → gray, monospace-styled text
     - ```` ```code blocks``` ```` → indented gray block
     - spoilers → `▮▮▮` with `hoverEvent=show_text(realText)`
   - **Mentions:**
     - `<@id>` → `LinkRepo.byDiscordId`. Linked + online → highlighted aqua,
       per-recipient sound (`minecraft:block.note_block.bell`). Linked +
       offline → cyan `@PlayerName`. Unknown → JDA member lookup →
       `@DisplayName`.
     - `<@&roleId>` → `@RoleName` in role color.
     - `@everyone` / `@here` → bold gold + soft ping for all online.
   - **Custom emoji** `<:name:id>` / `<a:name:id>` → `:name:` light purple
     with `hover_text=custom emoji`.
   - **Replies:** prefix line `┌─ replying to <author>: <first 40 chars…>` in
     dark gray, the whole quote `clickEvent=open_url(jumpUrl)`.
   - **Attachments:** per `Attachment.Kind`:
     - IMAGE → `[📷 image]` cyan, `hover=filename + size`, `click=open_url`
     - VIDEO → `[🎬 video.mp4]`
     - AUDIO → `[🔊 audio.mp3]`
     - FILE  → `[📎 filename.ext]`
   - **Embeds:** title in bold, description (truncated to 200 chars), each
     as quoted lines.
   - **Stickers:** `[sticker: name]` with hover showing URL.
5. `MinecraftPort.broadcast(scope, renderedText)` hops to the server thread
   via `server.execute(...)`. Per-recipient render path used when any
   mention requires per-player sound/highlight.

### 6.3 Edits, Deletes, Reactions

- **Edit (Discord):** `MessageUpdateEvent` → look up mirror in LRU. MC chat
  is append-only, so post a new message: `✏ <author> edited: <new body>` in
  italic gray. Original stays. Configurable to suppress.
- **Delete (Discord):** `MessageDeleteEvent` → `🗑 <author> deleted a message`
  in dark gray italic. Configurable to suppress.
- **Reactions:** `MessageReactionAddEvent` on a bridged message →
  `<reactor> reacted with <emoji> to <author>'s message`, gray, same scope.
  Default OFF.

### 6.4 Server Events → Discord

| Event       | Discord output                                                 |
| ----------- | -------------------------------------------------------------- |
| Join        | Embed, green border, head thumbnail, `Player joined the game`  |
| Leave       | Embed, gray border, head thumbnail, `Player left the game`     |
| Death       | Embed, red border, MC death message                            |
| Advancement | Embed, blue/purple border, `Player got the advancement [X]`    |
| Start       | `🟢 Server started`                                            |
| Stop        | `🔴 Server stopped`                                            |
| Crash       | `💥 Server crashed (exit code N)` — posted before JDA close    |

## 7. Account Linking

1. Player runs `/link` in MC.
2. `BridgeOrchestrator.startLink(playerUuid)`:
   - Generate 6-char code (uppercase alphanumeric, excluding `0`, `O`, `1`,
     `I`, `L`).
   - `LinkRepo.savePending(PendingLink(uuid, code, now+5min))`.
   - Send code to that player only, with `ClickEvent.COPY_TO_CLIPBOARD`.
3. Player runs `/link <code>` in Discord (slash command).
4. `BridgeOrchestrator.completeLink(discordId, code)`:
   - `LinkRepo.consumePending(code)` — atomic
     `DELETE ... RETURNING ...` returning `mcUuid` or empty.
   - Match → `LinkRepo.link(mcUuid, discordId)`. Ephemeral Discord reply
     `✅ Linked to <mcName>`. MC message to player `✅ Linked to Discord
     as <DiscordName>`.
   - Miss → ephemeral `❌ Invalid or expired code`.
5. `/unlink` (Discord) and `/unlink` (MC) both work; either side severs.
6. `/players` (Discord, ephemeral): online MC players + linked status.

### 7.1 MC commands (full list)

- `/link` — start a link flow (any player).
- `/unlink` — sever this player's link (any player; only their own row).
- `/dmcl status` — admin/op-only. Shows JDA connection state, channel
  health (per-scope OK / disabled / unauthorized), pending-link queue
  size, last 5 errors with timestamps. No PII.
- `/sc <message>` — only registered if a `STAFF`-scope channel is
  configured with a `mc_command`. Posts to the staff channel.

### 7.2 Discord slash commands (full list)

- `/link <code>` — complete a pending link.
- `/unlink` — sever this Discord user's link.
- `/players` — ephemeral list of online MC players + linked status.

## 8. Persistence

`SqliteLinkRepo` writes to `<world>/dmcl/links.db` (configurable to
`<gameDir>/dmcl/links.db` for a global, world-independent DB).

Single migration `V1__init.sql`:

```sql
CREATE TABLE linked_account (
  mc_uuid       TEXT PRIMARY KEY,
  discord_id    INTEGER NOT NULL UNIQUE,
  linked_at     INTEGER NOT NULL
);
CREATE TABLE pending_link (
  code          TEXT PRIMARY KEY,
  mc_uuid       TEXT NOT NULL UNIQUE,
  expires_at    INTEGER NOT NULL
);
CREATE INDEX idx_pending_expires ON pending_link(expires_at);
```

- `consumePending` is one transactional `DELETE ... RETURNING ...`.
- Sweeper task wipes expired pending rows every 60s on the orchestrator
  executor.
- No connection pool — single `Connection` reused, single-threaded access.
- `BridgeMessage` LRU (edit/delete correlation) is in-memory only, capped at
  5,000. Restart loses correlation; that is acceptable.
- `WebhookCache` is in-memory; on startup, list each channel's webhooks and
  find one named `DMCL Bridge` or create it.

## 9. Configuration

### 9.1 `<gameDir>/config/dmcl.toml`

```toml
[discord]
token            = "env:DMCL_DISCORD_TOKEN"
guild_id         = 123456789012345678
status           = "watching {online} players"

[storage]
db_path          = "world"   # "world" | "global"

[avatars]
provider         = "mc-heads"   # "mc-heads" | "crafatar" | "none"
size             = 64

[behavior]
show_edits        = true
show_deletes      = true
show_reactions    = false
mc_mention_color  = "aqua"
ping_sound        = "minecraft:block.note_block.bell"
loop_guard_strict = true

[[channels]]
scope          = "GLOBAL"
channel_id     = 100000000000000001
mc_format      = "<{name}> {message}"
discord_format = "{message}"

[[channels]]
scope          = "STAFF"
channel_id     = 100000000000000002
mc_permission  = "minecraft.command.op"
mc_command     = "sc"

[[channels]]
scope          = "DEATHS"
channel_id     = 100000000000000003
mc_send        = false

[[channels]]
scope          = "ADVANCEMENTS"
channel_id     = 100000000000000003
mc_send        = false

[[channels]]
scope          = "LIFECYCLE"
channel_id     = 100000000000000004
mc_send        = false

[mentions]
allow_everyone = false
allow_here     = false
allow_role     = ["MOD", "ADMIN"]
```

### 9.2 Token / secret resolution

A string starting with `env:NAME` is resolved at load time by trying, in
order:

1. Real OS environment variable `NAME`.
2. `<gameDir>/config/dmcl.env` (KEY=VALUE per line, `#` comments allowed).
3. `<gameDir>/config/dmcl.secrets.toml` (mirror of `dmcl.toml`'s shape, only
   secret keys).

First match wins. Missing → fail-fast at startup with a clear error naming
the missing key.

## 10. Threading Model

- One `ScheduledExecutorService` named `bridge-orchestrator`, single-thread,
  owns all bridge state (LRU, channel cache, pending-link sweep).
- **MC → orchestrator:** `ChatEventHook` builds a capture record on the
  server thread and `executor.submit()`s it. No MC API calls beyond reads.
- **JDA → orchestrator:** `JdaMessageListener` runs on JDA gateway thread,
  same submit pattern.
- **Orchestrator → Discord:** `RestAction.queue()` from orchestrator thread.
  JDA owns rate-limit threading.
- **Orchestrator → MC:** `MinecraftPort.broadcast` calls `server.execute(...)`
  so the actual `PlayerManager.broadcast` runs on the server thread.
- Sweeper: `executor.scheduleAtFixedRate(linkRepo::deleteExpiredPending, 60,
  60, SECONDS)`.
- Shutdown: `ServerLifecycleEvents.SERVER_STOPPING` → flush executor (5s
  grace) → close JDA → close SQLite. Crash hook posts the lifecycle embed
  before JDA close.

## 11. Error Handling (Railway-Oriented)

- All port methods that can fail return `Result<T, BridgeError>`.
  `BridgeError` is a sealed type with cases:
  `NetworkError`, `RateLimited(Duration retryAfter)`, `NotFound`,
  `BadInput(String reason)`, `Unauthorized`.
- Translation never throws — malformed Discord MD falls back to literal
  text with a debug log.
- `WebhookCache.send` retries once on `NetworkError`. On `RateLimited`,
  defers to JDA's queue. On `Unauthorized`, logs once and disables that
  channel until next config reload (no spam).
- No silent swallowing. Every `Result.Err` either retries, posts a console
  warning to MC ops, or surfaces in `/dmcl status` (a small admin-only MC
  command, see §7.1).
- Startup `ConfigValidator`:
  - Token present + non-empty.
  - Each `[[channels]]` `channel_id` resolves to a real text channel
    (collected, single error report).
  - Webhook permission present per channel; missing → log + skip that
    channel; do not crash the mod.

## 12. Loop Guard

Every webhook the mod creates is registered in `WebhookCache` with its
snowflake ID. `JdaMessageListener` drops any incoming message whose author
ID matches a known webhook ID. As a second layer, the listener also dedupes
by `(authorId, body, timestamp±2s)` LRU.

## 13. Build & Packaging

- Gradle 8.x + Loom 1.7.x.
- Java 21 (1.21.1 requirement).
- Yarn mappings (more common in Fabric ecosystem).
- Shaded into the mod jar: JDA 5.x, sqlite-jdbc, night-config (TOML),
  jakarta.validation. Fabric API + fabric-language-kotlin treated as
  external mod deps (already in target mods folder).
- `Spotless` (google-java-format), `ErrorProne`.
- Output: `build/libs/dmcl-<version>.jar`.
- Custom Gradle task `installMod` copies `build/libs/dmcl-*.jar` to
  `/mnt/c/dev/mc-server/mods/` after every successful build.

## 14. Testing Strategy

Per global `CLAUDE.md`: TDD London School, ≥80% coverage, all three of
unit / integration / e2e.

- **Unit (~85% coverage gate at 80%):** every `core/translate/*` against
  fixture inputs (markdown samples, mention shapes, attachment shapes).
  Pure functions, no mocks needed.
- **Orchestrator behavior tests:** mock all four ports, drive
  `BridgeOrchestrator` with synthetic events, assert collaborations.
  Example: `whenDiscordMessageReceived_withImageAttachment_callsMcBroadcastWithHyperlinkText()`.
- **Adapter integration:**
  - `SqliteLinkRepoTest` — in-memory SQLite, exercises pending-link race
    (concurrent consume → only one wins).
  - `JdaDiscordAdapterTest` — JDA listener wiring; webhook send mocked at
    HTTP level via WireMock.
  - `FabricMinecraftAdapterTest` — Fabric `gametest` smoke test that chat
    hook fires.
- **End-to-end:** one Fabric `gametest` Discord→MC, one MC→Discord, both
  using a fake JDA.
- **CI:** GitHub Actions, `./gradlew check` on push (test + Spotless +
  ErrorProne).

## 15. Future Work (out of v1 scope)

- Telegram / Matrix providers — would extract a `ChatProvider` SPI from the
  current single-Discord adapter set.
- Per-player Discord avatar override (let players link a custom avatar URL
  shown in webhook posts instead of their MC head).
- Webhook edit on MC `/edit` command (would require an MC-side message
  store, currently out of scope).
- Cross-shard chat aggregation (one Discord channel ↔ multiple MC servers).

## 16. Open Questions

None at design time. All decisions captured in §3.
