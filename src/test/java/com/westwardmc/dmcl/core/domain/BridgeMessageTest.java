package com.westwardmc.dmcl.core.domain;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class BridgeMessageTest {
    @Test
    void buildMcOriginMessage() {
        var author = new Author(Optional.of(UUID.randomUUID()), Optional.empty(), "Steve", "url");
        var msg = new BridgeMessage(
            "mc:1", Source.MINECRAFT, author, "hi", List.of(),
            Optional.empty(), Scope.GLOBAL, Instant.EPOCH, false);
        assertThat(msg.source()).isEqualTo(Source.MINECRAFT);
        assertThat(msg.body()).isEqualTo("hi");
    }

    @Test
    void emptyIdRejected() {
        var author = new Author(Optional.of(UUID.randomUUID()), Optional.empty(), "Steve", "url");
        assertThatThrownBy(() -> new BridgeMessage(
            "", Source.MINECRAFT, author, "hi", List.of(),
            Optional.empty(), Scope.GLOBAL, Instant.EPOCH, false))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pendingLinkExpiryComparison() {
        var p = new PendingLink(UUID.randomUUID(), "ABC123", Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(p.isExpired(Instant.parse("2026-01-01T00:00:01Z"))).isTrue();
        assertThat(p.isExpired(Instant.parse("2025-12-31T23:59:59Z"))).isFalse();
    }
}
