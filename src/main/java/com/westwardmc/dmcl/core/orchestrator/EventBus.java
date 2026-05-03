package com.westwardmc.dmcl.core.orchestrator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class EventBus implements AutoCloseable {
    private final ScheduledExecutorService exec;
    private volatile boolean closed = false;

    public EventBus(String name) {
        this.exec = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "dmcl-" + name);
            t.setDaemon(true);
            return t;
        });
    }

    public void submit(Runnable r) { if (!closed) exec.submit(r); }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initialDelay, long period, TimeUnit unit) {
        return exec.scheduleAtFixedRate(r, initialDelay, period, unit);
    }

    public synchronized void shutdown() {
        if (closed) return;
        closed = true;
        exec.shutdown();
        try {
            if (!exec.awaitTermination(5, TimeUnit.SECONDS)) exec.shutdownNow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            exec.shutdownNow();
        }
    }

    @Override public void close() { shutdown(); }
}
