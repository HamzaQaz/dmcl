package com.westwardmc.dmcl.adapter.jda;

import com.westwardmc.dmcl.core.domain.Attachment;
import com.westwardmc.dmcl.core.domain.Author;
import com.westwardmc.dmcl.core.domain.BridgeError;
import com.westwardmc.dmcl.core.domain.BridgeMessage;
import com.westwardmc.dmcl.core.domain.ReplyContext;
import com.westwardmc.dmcl.core.domain.Result;
import com.westwardmc.dmcl.core.domain.Scope;
import com.westwardmc.dmcl.core.port.ChannelMap;
import com.westwardmc.dmcl.core.port.DiscordPort;
import com.westwardmc.dmcl.core.port.PostedRef;
import com.westwardmc.dmcl.core.port.ReactionEvent;
import com.westwardmc.dmcl.core.port.SystemEvent;
import com.westwardmc.dmcl.core.translate.SystemEventRenderer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.awt.Color;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class JdaDiscordAdapter implements DiscordPort {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(JdaDiscordAdapter.class);

    private final JDA jda;
    private final WebhookCache webhooks;
    private final ChannelMap channels;
    private final HttpClient http = HttpClient.newHttpClient();

    private Consumer<BridgeMessage> inboundHandler = m -> {};
    private Consumer<ReactionEvent> reactionHandler = e -> {};

    public JdaDiscordAdapter(JDA jda, WebhookCache webhooks, ChannelMap channels) {
        this.jda = jda;
        this.webhooks = webhooks;
        this.channels = channels;
    }

    public WebhookCache webhooks() { return webhooks; }
    public Consumer<BridgeMessage> inboundHandler() { return inboundHandler; }
    public Consumer<ReactionEvent> reactionHandler() { return reactionHandler; }

    @Override
    public Result<PostedRef, BridgeError> sendWebhook(
        Scope scope, Author author, String body, List<Attachment> attachments, Optional<ReplyContext> reply) {
        var binding = channels.forScope(scope);
        if (binding.isEmpty()) return Result.err(new BridgeError.NotFound());
        WebhookCache.Ref ref;
        try {
            ref = webhooks.getOrCreate(binding.get().channelId());
        } catch (RuntimeException e) {
            return Result.err(new BridgeError.NetworkError(e.getMessage()));
        }

        String url = "https://discord.com/api/webhooks/" + ref.id() + "/" + ref.token() + "?wait=true";
        String payload = "{\"content\":" + jsonString(body)
            + ",\"username\":" + jsonString(author.displayName())
            + ",\"avatar_url\":" + jsonString(author.avatarUrl()) + "}";

        try {
            var req = HttpRequest.newBuilder(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 429) return Result.err(new BridgeError.RateLimited(Duration.ofSeconds(1)));
            if (resp.statusCode() == 401 || resp.statusCode() == 403) return Result.err(new BridgeError.Unauthorized());
            if (resp.statusCode() / 100 != 2) return Result.err(new BridgeError.NetworkError("HTTP " + resp.statusCode()));
            long mid = parseSnowflakeIdField(resp.body());
            return Result.ok(new PostedRef(ref.channelId(), mid, String.valueOf(ref.id()), ref.token()));
        } catch (Exception e) {
            LOG.warn("webhook send failed", e);
            return Result.err(new BridgeError.NetworkError(e.getMessage()));
        }
    }

    @Override
    public Result<Void, BridgeError> editWebhook(PostedRef ref, String newBody) {
        String url = "https://discord.com/api/webhooks/" + ref.webhookId() + "/" + ref.webhookToken()
            + "/messages/" + ref.messageId();
        return doPatchOrDelete(url, "PATCH", "{\"content\":" + jsonString(newBody) + "}");
    }

    @Override
    public Result<Void, BridgeError> deleteWebhook(PostedRef ref) {
        String url = "https://discord.com/api/webhooks/" + ref.webhookId() + "/" + ref.webhookToken()
            + "/messages/" + ref.messageId();
        return doPatchOrDelete(url, "DELETE", null);
    }

    private Result<Void, BridgeError> doPatchOrDelete(String url, String method, String body) {
        try {
            var b = HttpRequest.newBuilder(URI.create(url));
            if ("DELETE".equals(method)) b.DELETE();
            else b.method("PATCH", HttpRequest.BodyPublishers.ofString(body)).header("Content-Type", "application/json");
            var resp = http.send(b.build(), HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() / 100 == 2 ? Result.ok(null) : Result.err(new BridgeError.NetworkError("HTTP " + resp.statusCode()));
        } catch (Exception e) { return Result.err(new BridgeError.NetworkError(e.getMessage())); }
    }

    @Override
    public Result<Void, BridgeError> postSystem(Scope scope, SystemEvent ev) {
        var binding = channels.forScope(scope);
        if (binding.isEmpty()) return Result.err(new BridgeError.NotFound());
        TextChannel channel = jda.getTextChannelById(binding.get().channelId());
        if (channel == null) return Result.err(new BridgeError.NotFound());

        SystemEventRenderer.Card card = switch (ev) {
            case SystemEvent.Lifecycle l -> SystemEventRenderer.render(l.ev());
            case SystemEvent.Player p    -> SystemEventRenderer.render(p.ev());
        };

        var eb = new EmbedBuilder()
            .setTitle(card.title())
            .setColor(new Color(card.color()));
        if (!card.description().isEmpty()) eb.setDescription(card.description());
        card.headUuid().ifPresent(uuid ->
            eb.setThumbnail("https://mc-heads.net/avatar/" + uuid + "/64"));
        channel.sendMessageEmbeds(eb.build()).queue();
        return Result.ok(null);
    }

    @Override public void onInbound(Consumer<BridgeMessage> handler) { this.inboundHandler = handler; }
    @Override public void onReaction(Consumer<ReactionEvent> handler) { this.reactionHandler = handler; }

    @Override public void start() { /* JDA already connected externally */ }
    @Override public void shutdown() { jda.shutdown(); }

    private static String jsonString(String s) {
        if (s == null) return "null";
        return '"' + s.replace("\\", "\\\\").replace("\"", "\\\"")
                       .replace("\n", "\\n").replace("\r", "\\r")
                       .replace("\t", "\\t") + '"';
    }

    private static long parseSnowflakeIdField(String json) {
        int i = json.indexOf("\"id\"");
        if (i < 0) return 0;
        int colon = json.indexOf(':', i);
        int firstQuote = json.indexOf('"', colon + 1);
        int secondQuote = json.indexOf('"', firstQuote + 1);
        if (firstQuote < 0 || secondQuote < 0) return 0;
        try { return Long.parseLong(json.substring(firstQuote + 1, secondQuote)); }
        catch (NumberFormatException e) { return 0; }
    }
}
