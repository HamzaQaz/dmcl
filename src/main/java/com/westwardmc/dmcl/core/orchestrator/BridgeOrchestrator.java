package com.westwardmc.dmcl.core.orchestrator;

import com.westwardmc.dmcl.core.domain.*;
import com.westwardmc.dmcl.core.port.*;
import com.westwardmc.dmcl.core.translate.*;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class BridgeOrchestrator {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(BridgeOrchestrator.class);
    private static final char[] CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();
    private static final Duration LINK_TTL = Duration.ofMinutes(5);

    private final DiscordPort discord;
    private final MinecraftPort minecraft;
    private final LinkRepo links;
    private final ChannelMap channels;
    private final Clock clock;
    private final String pingSound;
    private final EventBus bus = new EventBus("orch");
    private final SecureRandom rng = new SecureRandom();

    public BridgeOrchestrator(DiscordPort discord, MinecraftPort minecraft, LinkRepo links,
                              ChannelMap channels, Clock clock, String pingSound) {
        this.discord = discord;
        this.minecraft = minecraft;
        this.links = links;
        this.channels = channels;
        this.clock = clock;
        this.pingSound = pingSound;
    }

    public void start() {
        minecraft.onChat(this::onMcChat);
        minecraft.onLifecycle(ev -> bus.submit(() -> discord.postSystem(Scope.LIFECYCLE, new SystemEvent.Lifecycle(ev))));
        minecraft.onPlayerEvent(ev -> bus.submit(() -> {
            Scope target = switch (ev) {
                case PlayerEvent.Died d       -> Scope.DEATHS;
                case PlayerEvent.Advanced a   -> Scope.ADVANCEMENTS;
                default                        -> Scope.GLOBAL;
            };
            discord.postSystem(target, new SystemEvent.Player(ev));
        }));
        discord.onInbound(this::onDiscordInbound);
        discord.onReaction(ev -> bus.submit(() -> LOG.debug("reaction event: {}", ev)));
        bus.scheduleAtFixedRate(() -> {
            try { links.deleteExpiredPending(clock.now()); }
            catch (Exception e) { LOG.warn("expired sweep failed", e); }
        }, 60, 60, TimeUnit.SECONDS);
    }

    public void shutdown() { bus.shutdown(); }

    private void onMcChat(BridgeMessage msg) {
        bus.submit(() -> {
            var resolver = new MentionResolver(links.all(), minecraft.getOnlinePlayers());
            String stripped = McToDiscord.stripColorCodes(msg.body());
            String escaped = McToDiscord.escapeMarkdown(stripped);
            String body = resolver.resolveOutbound(escaped);
            discord.sendWebhook(msg.scope(), msg.author(), body, msg.attachments(), msg.replyTo());
        });
    }

    private void onDiscordInbound(BridgeMessage msg) {
        bus.submit(() -> {
            var resolver = new MentionResolver(links.all(), minecraft.getOnlinePlayers());
            var renderer = new DiscordToMc(resolver, pingSound);
            var rendered = renderer.render(msg);
            minecraft.broadcast(msg.scope(), rendered);
        });
    }

    public void startLink(UUID mcUuid) {
        bus.submit(() -> {
            String code = generateCode();
            links.savePending(new PendingLink(mcUuid, code, clock.now().plus(LINK_TTL)));
            var msg = RenderedMcText.concat(
                RenderedMcText.colored("[DMCL] ", RenderedMcText.Color.AQUA),
                RenderedMcText.text("Run "),
                RenderedMcText.copyToClipboard("/link " + code, "Click to copy"),
                RenderedMcText.text(" in Discord. Code expires in 5 minutes.")
            );
            minecraft.sendTo(mcUuid, msg);
        });
    }

    public void completeLink(long discordId, String code) {
        bus.submit(() -> {
            var match = links.consumePending(code);
            if (match.isEmpty()) {
                discord.sendWebhook(Scope.GLOBAL,
                    new Author(Optional.empty(), Optional.of(0L), "DMCL", ""),
                    "❌ Invalid or expired code", List.of(), Optional.empty());
                return;
            }
            links.link(match.get(), discordId);
        });
    }

    private String generateCode() {
        var sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) sb.append(CODE_CHARS[rng.nextInt(CODE_CHARS.length)]);
        return sb.toString();
    }
}
