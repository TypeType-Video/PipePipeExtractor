package org.schabi.newpipe.extractor.services.youtube.extractors;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class YoutubeLiveChatPollingTask implements AutoCloseable {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private final Runnable task;
    private ScheduledFuture<?> future;
    private boolean closed;

    YoutubeLiveChatPollingTask(final Runnable task) {
        this.task = task;
    }

    synchronized void start() {
        if (!closed && future == null) {
            schedule();
        }
    }

    synchronized void pause() {
        if (future != null) {
            future.cancel(true);
        }
    }

    synchronized void resume() {
        if (!closed && future != null && future.isCancelled()) {
            schedule();
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        pause();
        executor.shutdownNow();
    }

    private void schedule() {
        future = executor.scheduleAtFixedRate(task, 1000, 1000, TimeUnit.MILLISECONDS);
    }
}
