package com.westwardmc.dmcl.core.domain;

import java.time.Instant;
import java.util.UUID;

public record PendingLink(UUID mcUuid, String code, Instant expiresAt) {
    public boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }
}
