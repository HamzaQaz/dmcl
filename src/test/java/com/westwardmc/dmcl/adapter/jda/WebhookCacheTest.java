package com.westwardmc.dmcl.adapter.jda;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.requests.restaction.WebhookAction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings({"unchecked", "rawtypes"})
final class WebhookCacheTest {
    @Test
    void reusesExistingDmclWebhook() {
        var jda = mock(JDA.class);
        var channel = mock(TextChannel.class);
        var existing = mock(Webhook.class);
        when(existing.getName()).thenReturn("DMCL Bridge");
        when(existing.getIdLong()).thenReturn(7777L);
        when(existing.getToken()).thenReturn("tok");
        var ra = mock(RestAction.class);
        when(channel.retrieveWebhooks()).thenReturn(ra);
        doAnswer(inv -> { ((Consumer<List<Webhook>>) inv.getArgument(0)).accept(List.of(existing)); return null; })
            .when(ra).queue(any(Consumer.class));
        when(jda.getTextChannelById(100L)).thenReturn(channel);

        var cache = new WebhookCache(jda);
        var found = cache.getOrCreate(100L);
        assertThat(found.id()).isEqualTo(7777L);
    }

    @Test
    void createsWebhookWhenMissing() {
        var jda = mock(JDA.class);
        var channel = mock(TextChannel.class);
        var ra = mock(RestAction.class);
        var newHook = mock(Webhook.class);
        when(newHook.getName()).thenReturn("DMCL Bridge");
        when(newHook.getIdLong()).thenReturn(8888L);
        when(newHook.getToken()).thenReturn("newtok");
        when(channel.retrieveWebhooks()).thenReturn(ra);
        doAnswer(inv -> { ((Consumer<List<Webhook>>) inv.getArgument(0)).accept(List.of()); return null; })
            .when(ra).queue(any(Consumer.class));
        var act = mock(WebhookAction.class);
        when(channel.createWebhook("DMCL Bridge")).thenReturn(act);
        doAnswer(inv -> { ((Consumer<Webhook>) inv.getArgument(0)).accept(newHook); return null; })
            .when(act).queue(any(Consumer.class));
        when(jda.getTextChannelById(100L)).thenReturn(channel);

        var cache = new WebhookCache(jda);
        var ref = cache.getOrCreate(100L);
        assertThat(ref.id()).isEqualTo(8888L);
        assertThat(cache.knownWebhookIds()).contains(8888L);
    }
}
