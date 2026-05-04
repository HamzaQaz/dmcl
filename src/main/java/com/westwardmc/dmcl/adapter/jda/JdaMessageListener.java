package com.westwardmc.dmcl.adapter.jda;

import com.westwardmc.dmcl.core.domain.Attachment;
import com.westwardmc.dmcl.core.domain.Author;
import com.westwardmc.dmcl.core.domain.BridgeMessage;
import com.westwardmc.dmcl.core.domain.ReplyContext;
import com.westwardmc.dmcl.core.domain.Source;
import com.westwardmc.dmcl.core.port.ChannelMap;
import com.westwardmc.dmcl.core.port.ReactionEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class JdaMessageListener extends ListenerAdapter {
    private final Consumer<BridgeMessage> inbound;
    private final Consumer<ReactionEvent> reactionInbound;
    private final WebhookCache webhooks;
    private final ChannelMap channels;

    public JdaMessageListener(Consumer<BridgeMessage> inbound, Consumer<ReactionEvent> reactionInbound,
                              WebhookCache webhooks, ChannelMap channels) {
        this.inbound = inbound;
        this.reactionInbound = reactionInbound;
        this.webhooks = webhooks;
        this.channels = channels;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent ev) {
        if (ev.isWebhookMessage() && webhooks.knownWebhookIds().contains(ev.getAuthor().getIdLong())) return;
        if (ev.getAuthor().isBot()) return;
        var binding = channels.forChannelId(ev.getChannel().getIdLong());
        if (binding.isEmpty()) return;
        var msg = ev.getMessage();

        Optional<ReplyContext> reply = Optional.empty();
        Message ref = msg.getReferencedMessage();
        if (ref != null) {
            reply = Optional.of(new ReplyContext(
                ref.getContentRaw(),
                ref.getAuthor() != null ? ref.getAuthor().getName() : "?",
                Optional.of(URI.create(ref.getJumpUrl()))));
        }

        var attachments = new ArrayList<Attachment>();
        for (var a : msg.getAttachments()) {
            attachments.add(new Attachment(classify(a), URI.create(a.getUrl()),
                a.getFileName(), a.getSize()));
        }

        var author = new Author(
            Optional.empty(),
            Optional.of(ev.getAuthor().getIdLong()),
            ev.getMember() != null ? ev.getMember().getEffectiveName() : ev.getAuthor().getName(),
            ev.getMember() != null ? ev.getMember().getEffectiveAvatarUrl() : ev.getAuthor().getEffectiveAvatarUrl());

        var bm = new BridgeMessage(
            "d:" + msg.getId(),
            Source.DISCORD,
            author,
            msg.getContentRaw() == null ? "" : msg.getContentRaw(),
            attachments,
            reply,
            binding.get().scope(),
            msg.getTimeCreated().toInstant(),
            false);
        inbound.accept(bm);
    }

    @Override
    public void onMessageUpdate(MessageUpdateEvent ev) {
        if (webhooks.knownWebhookIds().contains(ev.getAuthor().getIdLong())) return;
        var binding = channels.forChannelId(ev.getChannel().getIdLong());
        if (binding.isEmpty()) return;
        var msg = ev.getMessage();
        var author = new Author(
            Optional.empty(), Optional.of(ev.getAuthor().getIdLong()),
            ev.getMember() != null ? ev.getMember().getEffectiveName() : ev.getAuthor().getName(),
            ev.getMember() != null ? ev.getMember().getEffectiveAvatarUrl() : ev.getAuthor().getEffectiveAvatarUrl());
        inbound.accept(new BridgeMessage(
            "d:" + msg.getId(), Source.DISCORD, author,
            msg.getContentRaw() == null ? "" : msg.getContentRaw(),
            List.of(), Optional.empty(),
            binding.get().scope(), Instant.now(), true));
    }

    @Override
    public void onMessageDelete(MessageDeleteEvent ev) {
        var binding = channels.forChannelId(ev.getChannel().getIdLong());
        if (binding.isEmpty()) return;
        var author = new Author(Optional.empty(), Optional.of(0L), "(unknown)", "");
        inbound.accept(new BridgeMessage(
            "d:" + ev.getMessageId(), Source.DISCORD, author,
            "(deleted message)", List.of(), Optional.empty(),
            binding.get().scope(), Instant.now(), true));
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent ev) {
        reactionInbound.accept(new ReactionEvent(
            "d:" + ev.getMessageId(),
            ev.getUserIdLong(),
            ev.getMember() != null ? ev.getMember().getEffectiveName() : "?",
            ev.getEmoji().getName()));
    }

    private Attachment.Kind classify(Message.Attachment a) {
        if (a.isImage()) return Attachment.Kind.IMAGE;
        if (a.isVideo()) return Attachment.Kind.VIDEO;
        String ct = a.getContentType();
        if (ct != null && ct.startsWith("audio/")) return Attachment.Kind.AUDIO;
        return Attachment.Kind.FILE;
    }
}
