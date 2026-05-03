package com.westwardmc.dmcl.core.orchestrator;

import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.assertj.core.api.Assertions.assertThat;

final class EventBusTest {
    @Test
    void runsTaskOnSingleThread() throws Exception {
        var bus = new EventBus("test");
        var threadId = new java.util.concurrent.atomic.AtomicReference<Long>();
        var done = new CountDownLatch(1);
        bus.submit(() -> { threadId.set(Thread.currentThread().getId()); done.countDown(); });
        assertThat(done.await(2, TimeUnit.SECONDS)).isTrue();
        long first = threadId.get();
        var done2 = new CountDownLatch(1);
        bus.submit(() -> { threadId.set(Thread.currentThread().getId()); done2.countDown(); });
        done2.await(2, TimeUnit.SECONDS);
        assertThat(threadId.get()).isEqualTo(first);
        bus.shutdown();
    }

    @Test
    void scheduledTaskFires() throws Exception {
        var bus = new EventBus("test");
        var counter = new AtomicInteger();
        bus.scheduleAtFixedRate(counter::incrementAndGet, 50, 50, TimeUnit.MILLISECONDS);
        Thread.sleep(220);
        bus.shutdown();
        assertThat(counter.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void shutdownIsIdempotent() {
        var bus = new EventBus("test");
        bus.shutdown();
        bus.shutdown();
    }
}
