package com.westwardmc.dmcl.adapter.jda;

import com.westwardmc.dmcl.core.domain.Author;
import com.westwardmc.dmcl.core.domain.BridgeError;
import com.westwardmc.dmcl.core.domain.Scope;
import com.westwardmc.dmcl.core.port.ChannelBinding;
import com.westwardmc.dmcl.core.port.ChannelMap;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class JdaDiscordAdapterTest {
    JDA jda;
    WebhookCache webhooks;
    ChannelMap channels;
    TextChannel ch;

    @BeforeEach
    void setup() {
        jda = mock(JDA.class);
        webhooks = mock(WebhookCache.class);
        channels = mock(ChannelMap.class);
        ch = mock(TextChannel.class);
        when(jda.getTextChannelById(100L)).thenReturn(ch);
        when(channels.forScope(Scope.GLOBAL)).thenReturn(Optional.of(
            new ChannelBinding(Scope.GLOBAL, 100L, "<{name}> {message}", "{message}",
                Optional.empty(), Optional.empty(), true)));
    }

    @Test
    void sendWebhookFailsWhenNoBindingForScope() {
        when(channels.forScope(Scope.GLOBAL)).thenReturn(Optional.empty());
        var adapter = new JdaDiscordAdapter(jda, webhooks, channels);
        var author = new Author(Optional.of(UUID.randomUUID()), Optional.empty(), "Steve", "url");
        var r = adapter.sendWebhook(Scope.GLOBAL, author, "hi", List.of(), Optional.empty());
        assertThat(r.isOk()).isFalse();
        assertThat(r.unwrapErr()).isInstanceOf(BridgeError.NotFound.class);
    }

    @Test
    void sendWebhookSurfacesWebhookCacheError() {
        when(webhooks.getOrCreate(100L)).thenThrow(new RuntimeException("channel missing"));
        var adapter = new JdaDiscordAdapter(jda, webhooks, channels);
        var author = new Author(Optional.of(UUID.randomUUID()), Optional.empty(), "Steve", "https://x");
        var r = adapter.sendWebhook(Scope.GLOBAL, author, "hi", List.of(), Optional.empty());
        assertThat(r.isOk()).isFalse();
        assertThat(r.unwrapErr()).isInstanceOf(BridgeError.NetworkError.class);
    }
}
