package com.westwardmc.dmcl.core.orchestrator;

import com.westwardmc.dmcl.core.domain.*;
import com.westwardmc.dmcl.core.port.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

final class BridgeOrchestratorTest {
    DiscordPort discord;
    MinecraftPort minecraft;
    LinkRepo links;
    ChannelMap channels;
    Clock clock;
    BridgeOrchestrator orch;
    Consumer<BridgeMessage> mcChatHandler;
    Consumer<BridgeMessage> discordInboundHandler;

    @BeforeEach
    void setup() {
        discord = mock(DiscordPort.class);
        minecraft = mock(MinecraftPort.class);
        links = mock(LinkRepo.class);
        channels = mock(ChannelMap.class);
        clock = () -> Instant.parse("2026-01-01T00:00:00Z");

        when(channels.forScope(any())).thenReturn(Optional.of(
            new ChannelBinding(Scope.GLOBAL, 100L, "<{name}> {message}", "{message}",
                Optional.empty(), Optional.empty(), true)));
        when(minecraft.getOnlinePlayers()).thenReturn(Set.of());
        when(links.all()).thenReturn(List.of());

        orch = new BridgeOrchestrator(discord, minecraft, links, channels, clock,
            "minecraft:block.note_block.bell");
        orch.start();

        var mcCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(minecraft).onChat(mcCaptor.capture());
        @SuppressWarnings("unchecked")
        Consumer<BridgeMessage> mc = (Consumer<BridgeMessage>) mcCaptor.getValue();
        mcChatHandler = mc;

        var dCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(discord).onInbound(dCaptor.capture());
        @SuppressWarnings("unchecked")
        Consumer<BridgeMessage> d = (Consumer<BridgeMessage>) dCaptor.getValue();
        discordInboundHandler = d;
    }

    @AfterEach
    void tearDown() {
        if (orch != null) orch.shutdown();
    }

    private BridgeMessage mcMsg(String body) {
        var author = new Author(Optional.of(UUID.randomUUID()), Optional.empty(), "Steve", "url");
        return new BridgeMessage("mc:1", Source.MINECRAFT, author, body, List.of(),
            Optional.empty(), Scope.GLOBAL, Instant.EPOCH, false);
    }

    private BridgeMessage discordMsg(String body, List<Attachment> atts) {
        var author = new Author(Optional.empty(), Optional.of(99L), "Bob", "url");
        return new BridgeMessage("d:1", Source.DISCORD, author, body, atts,
            Optional.empty(), Scope.GLOBAL, Instant.EPOCH, false);
    }

    @Test
    void mcChatTriggersDiscordWebhook() throws Exception {
        when(discord.sendWebhook(any(), any(), any(), any(), any()))
            .thenReturn(Result.ok(new PostedRef(100L, 200L, "wid", "wt")));
        mcChatHandler.accept(mcMsg("hello"));
        verify(discord, timeout(1000)).sendWebhook(eq(Scope.GLOBAL), any(Author.class), eq("hello"),
            anyList(), eq(Optional.empty()));
    }

    @Test
    void mcChatEscapesMarkdownBeforePosting() throws Exception {
        when(discord.sendWebhook(any(), any(), any(), any(), any()))
            .thenReturn(Result.ok(new PostedRef(100L, 200L, "wid", "wt")));
        mcChatHandler.accept(mcMsg("*test*"));
        verify(discord, timeout(1000)).sendWebhook(any(), any(), eq("\\*test\\*"), any(), any());
    }

    @Test
    void discordMessageBroadcastsToMc() throws Exception {
        discordInboundHandler.accept(discordMsg("hi", List.of()));
        verify(minecraft, timeout(1000)).broadcast(eq(Scope.GLOBAL), any(RenderedMcText.class));
    }

    @Test
    void discordImageAttachmentBroadcastsHyperlinkToMc() throws Exception {
        var att = new Attachment(Attachment.Kind.IMAGE, URI.create("https://x/y.png"), "y.png", 100);
        discordInboundHandler.accept(discordMsg("look", List.of(att)));
        var captor = ArgumentCaptor.forClass(RenderedMcText.class);
        verify(minecraft, timeout(1000)).broadcast(eq(Scope.GLOBAL), captor.capture());
        boolean hasLink = captor.getValue().spans().stream()
            .anyMatch(s -> s instanceof RenderedMcText.Span.Hyperlink);
        assertThat(hasLink).isTrue();
    }

    @Test
    void startLinkSavesPendingAndSendsCodeToPlayer() {
        var uuid = UUID.randomUUID();
        orch.startLink(uuid);
        verify(links, timeout(1000)).savePending(any(PendingLink.class));
        verify(minecraft, timeout(1000)).sendTo(eq(uuid), any(RenderedMcText.class));
    }

    @Test
    void completeLinkPersistsOnSuccess() {
        when(links.consumePending("ABC123")).thenReturn(Optional.of(UUID.randomUUID()));
        orch.completeLink(99L, "ABC123");
        verify(links, timeout(1000)).link(any(UUID.class), eq(99L));
    }

    @Test
    void completeLinkNoOpOnInvalidCode() {
        when(links.consumePending(anyString())).thenReturn(Optional.empty());
        orch.completeLink(99L, "BAD");
        verify(links, after(200).never()).link(any(), anyLong());
    }
}
