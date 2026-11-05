package org.schabi.newpipe.extractor.utils;

import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class BackgroundRequests {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool(runnable -> {
        final Thread thread = new Thread(runnable, "pipepipe-background-request");
        thread.setDaemon(true);
        return thread;
    });

    private BackgroundRequests() {
    }

    public static <T> Future<T> start(final Callable<T> request) {
        return EXECUTOR.submit(request);
    }

    public static <T> T await(final Future<T> request) throws IOException, ExtractionException {
        try {
            return request.get();
        } catch (final InterruptedException e) {
            request.cancel(true);
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for a background request", e);
        } catch (final ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException) cause;
            }
            if (cause instanceof ExtractionException) {
                throw (ExtractionException) cause;
            }
            throw new ExtractionException(cause);
        }
    }
}
