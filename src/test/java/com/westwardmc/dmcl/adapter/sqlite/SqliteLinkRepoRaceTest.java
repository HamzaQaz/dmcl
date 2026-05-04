package com.westwardmc.dmcl.adapter.sqlite;

import com.westwardmc.dmcl.core.domain.PendingLink;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

final class SqliteLinkRepoRaceTest {
    @Test
    void concurrentConsumeOnlyOneWins() throws Exception {
        var dbFile = Files.createTempFile("dmcl-race", ".db");
        var repo1 = new SqliteLinkRepo("jdbc:sqlite:" + dbFile.toAbsolutePath());
        var repo2 = new SqliteLinkRepo("jdbc:sqlite:" + dbFile.toAbsolutePath());
        try {
            repo1.migrate();
            var uuid = UUID.randomUUID();
            repo1.savePending(new PendingLink(uuid, "RACE99", Instant.parse("2099-01-01T00:00:00Z")));

            var pool = Executors.newFixedThreadPool(2);
            var winners = new AtomicInteger();
            var latch = new CountDownLatch(1);
            Callable<Boolean> attempt1 = () -> { latch.await(); return repo1.consumePending("RACE99").isPresent(); };
            Callable<Boolean> attempt2 = () -> { latch.await(); return repo2.consumePending("RACE99").isPresent(); };
            var f1 = pool.submit(attempt1);
            var f2 = pool.submit(attempt2);
            latch.countDown();
            if (f1.get(5, TimeUnit.SECONDS)) winners.incrementAndGet();
            if (f2.get(5, TimeUnit.SECONDS)) winners.incrementAndGet();
            pool.shutdown();
            assertThat(winners.get()).isEqualTo(1);
        } finally {
            repo1.close();
            repo2.close();
            Files.deleteIfExists(dbFile);
        }
    }
}
