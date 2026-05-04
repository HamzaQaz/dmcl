package com.westwardmc.dmcl.adapter.sqlite;

import com.westwardmc.dmcl.core.domain.PendingLink;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

final class SqliteLinkRepoTest {
    private SqliteLinkRepo repo;

    @BeforeEach void setup() throws Exception { repo = new SqliteLinkRepo("jdbc:sqlite::memory:"); repo.migrate(); }
    @AfterEach  void teardown() throws Exception { repo.close(); }

    @Test
    void savePendingThenConsumeReturnsUuid() {
        var uuid = UUID.randomUUID();
        repo.savePending(new PendingLink(uuid, "ABC123", Instant.parse("2026-12-31T00:00:00Z")));
        assertThat(repo.consumePending("ABC123")).contains(uuid);
        assertThat(repo.consumePending("ABC123")).isEmpty();
    }

    @Test
    void linkAndLookup() {
        var uuid = UUID.randomUUID();
        repo.link(uuid, 999L);
        assertThat(repo.byMcUuid(uuid)).isPresent();
        assertThat(repo.byDiscordId(999L)).isPresent();
        assertThat(repo.byDiscordId(999L).get().mcUuid()).isEqualTo(uuid);
    }

    @Test
    void unlinkRemovesRow() {
        var uuid = UUID.randomUUID();
        repo.link(uuid, 1L);
        repo.unlinkByMc(uuid);
        assertThat(repo.byMcUuid(uuid)).isEmpty();
    }

    @Test
    void deleteExpiredPendingPurgesPastEntries() {
        repo.savePending(new PendingLink(UUID.randomUUID(), "OLD111", Instant.parse("2020-01-01T00:00:00Z")));
        repo.savePending(new PendingLink(UUID.randomUUID(), "NEW222", Instant.parse("2099-01-01T00:00:00Z")));
        int deleted = repo.deleteExpiredPending(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(deleted).isEqualTo(1);
        assertThat(repo.consumePending("OLD111")).isEmpty();
        assertThat(repo.consumePending("NEW222")).isPresent();
    }
}
