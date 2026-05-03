package com.westwardmc.dmcl.core.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public record BridgeMessage(
    String id,
    Source source,
    Author author,
    String body,
    List<Attachment> attachments,
    Optional<ReplyContext> replyTo,
    Scope scope,
    Instant timestamp,
    boolean edited
) {
    public BridgeMessage {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id required");
        if (body == null) throw new IllegalArgumentException("body required (may be empty if attachments)");
        attachments = List.copyOf(attachments);
    }
}
