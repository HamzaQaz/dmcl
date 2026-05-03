package com.westwardmc.dmcl.core.translate;

import com.westwardmc.dmcl.core.domain.*;
import com.westwardmc.dmcl.core.port.OnlinePlayer;
import org.junit.jupiter.api.Test;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

final class DiscordToMcTest {
    private BridgeMessage msg(String body, List<Attachment> atts, Optional<ReplyContext> reply) {
        var author = new Author(Optional.empty(), Optional.of(99L), "Bob", "url");
        return new BridgeMessage("d:1", Source.DISCORD, author, body, atts, reply, Scope.GLOBAL, Instant.EPOCH, false);
    }

    private DiscordToMc xlate(List<LinkedAccount> linked, Set<OnlinePlayer> online) {
        var resolver = new MentionResolver(linked, online);
        return new DiscordToMc(resolver, "minecraft:block.note_block.bell");
    }

    @Test
    void plainBodyEmitsHeader() {
        var t = xlate(List.of(), Set.of()).render(msg("hello", List.of(), Optional.empty()));
        assertThat(t.spans()).isNotEmpty();
        assertThat(((RenderedMcText.Span.Plain) t.spans().get(0)).text()).contains("Bob");
    }

    @Test
    void linkedDiscordIdGetsPingSpan() {
        var STEVE = UUID.randomUUID();
        var linked = List.of(new LinkedAccount(STEVE, 1234L, Instant.EPOCH));
        var online = Set.of(new OnlinePlayer(STEVE, "Steve"));
        var t = xlate(linked, online).render(msg("hi <@1234>", List.of(), Optional.empty()));
        boolean hasPing = t.spans().stream().anyMatch(s -> s instanceof RenderedMcText.Span.Ping);
        assertThat(hasPing).isTrue();
    }

    @Test
    void unknownMentionFallsBackToTextWithAt() {
        var t = xlate(List.of(), Set.of()).render(msg("hi <@999>", List.of(), Optional.empty()));
        boolean hasUnknown = t.spans().stream()
            .filter(s -> s instanceof RenderedMcText.Span.Plain)
            .map(s -> ((RenderedMcText.Span.Plain) s).text())
            .anyMatch(txt -> txt.contains("@unknown"));
        assertThat(hasUnknown).isTrue();
    }

    @Test
    void roleMentionRendersWithAtPrefix() {
        var t = xlate(List.of(), Set.of()).render(msg("ping <@&5>", List.of(), Optional.empty()));
        boolean hasRole = t.spans().stream()
            .filter(s -> s instanceof RenderedMcText.Span.Plain)
            .map(s -> ((RenderedMcText.Span.Plain) s).text())
            .anyMatch(txt -> txt.contains("@role"));
        assertThat(hasRole).isTrue();
    }

    @Test
    void customEmojiRewrittenToColon() {
        var t = xlate(List.of(), Set.of()).render(msg("yay <:party:123>", List.of(), Optional.empty()));
        boolean hasEmoji = t.spans().stream()
            .filter(s -> s instanceof RenderedMcText.Span.Plain)
            .map(s -> ((RenderedMcText.Span.Plain) s).text())
            .anyMatch(txt -> txt.contains(":party:"));
        assertThat(hasEmoji).isTrue();
    }

    @Test
    void attachmentAppended() {
        var a = new Attachment(Attachment.Kind.IMAGE, URI.create("https://x/y.png"), "y.png", 100);
        var t = xlate(List.of(), Set.of()).render(msg("look", List.of(a), Optional.empty()));
        boolean hasLink = t.spans().stream().anyMatch(s -> s instanceof RenderedMcText.Span.Hyperlink);
        assertThat(hasLink).isTrue();
    }

    @Test
    void replyPrefixPrependedAsQuotedSpan() {
        var reply = new ReplyContext("original snippet", "Alice", Optional.of(URI.create("https://discord.com/x")));
        var t = xlate(List.of(), Set.of()).render(msg("reply body", List.of(), Optional.of(reply)));
        boolean hasQuoted = t.spans().stream().anyMatch(s -> s instanceof RenderedMcText.Span.Quoted);
        assertThat(hasQuoted).isTrue();
    }
}
