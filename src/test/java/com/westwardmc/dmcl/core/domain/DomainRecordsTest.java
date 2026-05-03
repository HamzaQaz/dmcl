package com.westwardmc.dmcl.core.domain;

import org.junit.jupiter.api.Test;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class DomainRecordsTest {
    @Test
    void authorRequiresAtLeastOneIdentity() {
        assertThatThrownBy(() ->
            new Author(Optional.empty(), Optional.empty(), "name", "url"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one of");
    }

    @Test
    void mcAuthorOk() {
        var a = new Author(Optional.of(UUID.randomUUID()), Optional.empty(), "Steve", "x");
        assertThat(a.displayName()).isEqualTo("Steve");
    }

    @Test
    void attachmentClassifiesByKind() {
        var a = new Attachment(Attachment.Kind.IMAGE, URI.create("https://x/y.png"), "y.png", 1234);
        assertThat(a.kind()).isEqualTo(Attachment.Kind.IMAGE);
    }

    @Test
    void mentionTokenUserHasId() {
        var u = new MentionToken.User(123L);
        assertThat(u.discordId()).isEqualTo(123L);
    }

    @Test
    void replyContextIsImmutable() {
        var r = new ReplyContext("hello", "Steve", Optional.of(URI.create("https://discord.com/x")));
        assertThat(r.snippet()).isEqualTo("hello");
        assertThat(r.jumpUrl()).isPresent();
    }
}
