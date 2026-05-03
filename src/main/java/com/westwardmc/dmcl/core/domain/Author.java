package com.westwardmc.dmcl.core.domain;

import java.util.Optional;
import java.util.UUID;

public record Author(
    Optional<UUID> mcUuid,
    Optional<Long> discordId,
    String displayName,
    String avatarUrl
) {
    public Author {
        if (mcUuid.isEmpty() && discordId.isEmpty()) {
            throw new IllegalArgumentException("Author must have at least one of mcUuid or discordId");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("displayName required");
        }
    }
}
