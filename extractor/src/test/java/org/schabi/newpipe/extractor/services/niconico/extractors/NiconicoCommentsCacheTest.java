package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NiconicoCommentsCacheTest {
    private static final String THREADS = "{\"data\":{\"threads\":[{\"comments\":"
            + "[{\"no\":1,\"body\":\"first\"},{\"no\":2,\"body\":\"second\"}]}]}}";

    @Test
    @Timeout(5)
    void concurrentCallersWaitForTheCommentsBeingFetched() throws Exception {
        final BlockingDownloader downloader = new BlockingDownloader();
        final NiconicoCommentsCache cache = new NiconicoCommentsCache();
        final JsonObject watch = watch();
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            final Future<JsonObject[]> first =
                    executor.submit(() -> cache.getComments(watch, downloader, "sm9"));
            assertTrue(downloader.requestStarted.await(2, TimeUnit.SECONDS));
            final CountDownLatch secondStarted = new CountDownLatch(1);
            final Future<JsonObject[]> second = executor.submit(() -> {
                secondStarted.countDown();
                return cache.getComments(watch, downloader, "sm9");
            });
            assertTrue(secondStarted.await(2, TimeUnit.SECONDS));
            Thread.sleep(100);

            assertFalse(second.isDone());
            downloader.requestRelease.countDown();

            final JsonObject[] comments = first.get(2, TimeUnit.SECONDS);
            assertEquals(2, comments.length);
            assertSame(comments, second.get(2, TimeUnit.SECONDS));
            assertEquals(1, downloader.requestCount.get());
        } finally {
            downloader.requestRelease.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void failedFetchDoesNotLeaveEmptyCommentsBehind() throws Exception {
        final FlakyDownloader downloader = new FlakyDownloader();
        final NiconicoCommentsCache cache = new NiconicoCommentsCache();
        final JsonObject watch = watch();

        assertThrows(ExtractionException.class,
                () -> cache.getComments(watch, downloader, "sm9"));
        final JsonObject[] comments = cache.getComments(watch, downloader, "sm9");

        assertNotNull(comments);
        assertEquals(2, comments.length);
        assertEquals(2, downloader.requestCount.get());
    }

    private static JsonObject watch() throws Exception {
        return JsonParser.object().from(
                "{\"comment\":{\"nvComment\":{\"threadKey\":\"key\",\"params\":{}}}}");
    }

    private static Response threads() {
        return new Response(200, "OK", Collections.emptyMap(), THREADS, null,
                "https://public.nvcomment.nicovideo.jp/v1/threads");
    }

    private static final class BlockingDownloader extends Downloader {
        private final CountDownLatch requestStarted = new CountDownLatch(1);
        private final CountDownLatch requestRelease = new CountDownLatch(1);
        private final AtomicInteger requestCount = new AtomicInteger();

        @Override
        public Response execute(@Nonnull final Request request) throws IOException {
            requestCount.incrementAndGet();
            requestStarted.countDown();
            try {
                requestRelease.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted comments fixture", e);
            }
            return threads();
        }

        @Override
        public CancellableCall executeAsync(@Nonnull final Request request,
                                            final AsyncCallback callback) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FlakyDownloader extends Downloader {
        private final AtomicInteger requestCount = new AtomicInteger();

        @Override
        public Response execute(@Nonnull final Request request) throws IOException {
            if (requestCount.incrementAndGet() == 1) {
                throw new IOException("NicoNico did not answer");
            }
            return threads();
        }

        @Override
        public CancellableCall executeAsync(@Nonnull final Request request,
                                            final AsyncCallback callback) {
            throw new UnsupportedOperationException();
        }
    }
}
