package com.westwardmc.dmcl.core.port;

import com.westwardmc.dmcl.core.domain.LinkedAccount;
import com.westwardmc.dmcl.core.domain.PendingLink;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepo {
    void savePending(PendingLink p);
    Optional<UUID> consumePending(String code);
    void link(UUID mcUuid, long discordId);
    void unlinkByMc(UUID mcUuid);
    void unlinkByDiscord(long discordId);
    Optional<LinkedAccount> byMcUuid(UUID mcUuid);
    Optional<LinkedAccount> byDiscordId(long discordId);
    List<LinkedAccount> all();
    int deleteExpiredPending(Instant now);
}
