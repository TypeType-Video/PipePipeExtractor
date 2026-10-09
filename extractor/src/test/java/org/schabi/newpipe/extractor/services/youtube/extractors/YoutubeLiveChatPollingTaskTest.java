package org.schabi.newpipe.extractor.services.youtube.extractors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(10)
class YoutubeLiveChatPollingTaskTest {
    @Test
    void pauseCanResumeButCloseTerminatesTheThread() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicReference<Thread> thread = new AtomicReference<>();
        final CountDownLatch started = new CountDownLatch(1);
        final YoutubeLiveChatPollingTask polling = new YoutubeLiveChatPollingTask(() -> {
            thread.set(Thread.currentThread());
            calls.incrementAndGet();
            started.countDown();
        });
        try {
            polling.start();
            polling.start();
            assertTrue(started.await(3, TimeUnit.SECONDS));
            polling.pause();
            final int pausedCalls = calls.get();
            Thread.sleep(1100);
            assertEquals(pausedCalls, calls.get());
            polling.resume();
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (calls.get() == pausedCalls && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            assertTrue(calls.get() > pausedCalls);
            polling.close();
            thread.get().join(2000);
            assertFalse(thread.get().isAlive());
            assertDoesNotThrow(polling::resume);
            assertDoesNotThrow(polling::start);
            assertDoesNotThrow(polling::close);
        } finally {
            polling.close();
        }
    }

    @Test
    void closeInterruptsAnInFlightPoll() throws Exception {
        final CountDownLatch started = new CountDownLatch(1);
        final CountDownLatch stopped = new CountDownLatch(1);
        final YoutubeLiveChatPollingTask polling = new YoutubeLiveChatPollingTask(() -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
            } finally {
                stopped.countDown();
            }
        });
        try {
            polling.start();
            assertTrue(started.await(3, TimeUnit.SECONDS));
            polling.close();
            assertTrue(stopped.await(2, TimeUnit.SECONDS));
        } finally {
            polling.close();
        }
    }

    @Test
    void closingBeforeInitializationPreventsScheduling() {
        final AtomicInteger calls = new AtomicInteger();
        final YoutubeLiveChatPollingTask polling = new YoutubeLiveChatPollingTask(calls::incrementAndGet);
        polling.close();
        assertDoesNotThrow(polling::start);
        assertDoesNotThrow(polling::resume);
        assertEquals(0, calls.get());
    }
}
