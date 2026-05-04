package com.westwardmc.dmcl.adapter.jda;

import com.westwardmc.dmcl.core.domain.BridgeMessage;
import com.westwardmc.dmcl.core.domain.Scope;
import com.westwardmc.dmcl.core.port.ChannelBinding;
import com.westwardmc.dmcl.core.port.ChannelMap;
import com.westwardmc.dmcl.core.port.ReactionEvent;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class JdaMessageListenerTest {
    @Test
    void plainUserMessageDispatchedAsBridgeMessage() {
        var inbound = new AtomicReference<BridgeMessage>();
        var webhooks = mock(WebhookCache.class);
        when(webhooks.knownWebhookIds()).thenReturn(Set.of());
        var channels = mock(ChannelMap.class);
        when(channels.forChannelId(100L)).thenReturn(Optional.of(
            new ChannelBinding(Scope.GLOBAL, 100L, "<{name}> {message}", "{message}",
                Optional.empty(), Optional.empty(), true)));

        var listener = new JdaMessageListener(inbound::set, e -> {}, webhooks, channels);

        var ev = mock(MessageReceivedEvent.class);
        var msg = mock(Message.class);
        var author = mock(User.class);
        var member = mock(Member.class);
        var channelUnion = mock(MessageChannelUnion.class);
        when(ev.getMessage()).thenReturn(msg);
        when(ev.getAuthor()).thenReturn(author);
        when(ev.getMember()).thenReturn(member);
        when(ev.isWebhookMessage()).thenReturn(false);
        when(ev.getChannel()).thenReturn(channelUnion);
        when(channelUnion.getIdLong()).thenReturn(100L);
        when(author.isBot()).thenReturn(false);
        when(author.getIdLong()).thenReturn(99L);
        when(member.getEffectiveName()).thenReturn("Bob");
        when(member.getEffectiveAvatarUrl()).thenReturn("https://av");
        when(msg.getContentRaw()).thenReturn("hello");
        when(msg.getId()).thenReturn("12345");
        when(msg.getAttachments()).thenReturn(List.of());
        when(msg.getTimeCreated()).thenReturn(OffsetDateTime.now());
        when(msg.getReferencedMessage()).thenReturn(null);

        listener.onMessageReceived(ev);

        assertThat(inbound.get()).isNotNull();
        assertThat(inbound.get().body()).isEqualTo("hello");
        assertThat(inbound.get().scope()).isEqualTo(Scope.GLOBAL);
    }

    @Test
    void webhookMessageFromOurWebhookDropped() {
        var inbound = new AtomicReference<BridgeMessage>();
        var webhooks = mock(WebhookCache.class);
        when(webhooks.knownWebhookIds()).thenReturn(Set.of(7777L));
        var channels = mock(ChannelMap.class);
        var listener = new JdaMessageListener(inbound::set, e -> {}, webhooks, channels);

        var ev = mock(MessageReceivedEvent.class);
        var author = mock(User.class);
        when(ev.isWebhookMessage()).thenReturn(true);
        when(ev.getAuthor()).thenReturn(author);
        when(author.getIdLong()).thenReturn(7777L);

        listener.onMessageReceived(ev);
        assertThat(inbound.get()).isNull();
    }

    @Test
    void botMessageIgnored() {
        var inbound = new AtomicReference<BridgeMessage>();
        var webhooks = mock(WebhookCache.class);
        when(webhooks.knownWebhookIds()).thenReturn(Set.of());
        var channels = mock(ChannelMap.class);
        var listener = new JdaMessageListener(inbound::set, e -> {}, webhooks, channels);

        var ev = mock(MessageReceivedEvent.class);
        var author = mock(User.class);
        when(ev.isWebhookMessage()).thenReturn(false);
        when(ev.getAuthor()).thenReturn(author);
        when(author.isBot()).thenReturn(true);

        listener.onMessageReceived(ev);
        assertThat(inbound.get()).isNull();
    }
}
