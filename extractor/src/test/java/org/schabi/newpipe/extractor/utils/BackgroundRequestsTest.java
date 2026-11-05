package org.schabi.newpipe.extractor.utils;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackgroundRequestsTest {
    @Test
    void returnsTheValueOfARequestStartedEarlier() throws Exception {
        final CountDownLatch started = new CountDownLatch(1);
        final Future<String> request = BackgroundRequests.start(() -> {
            started.countDown();
            return "related";
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertEquals("related", BackgroundRequests.await(request));
    }

    @Test
    void keepsTheOriginalIoException() {
        final IOException failure = new IOException("timeout");
        final Future<String> request = BackgroundRequests.start(() -> {
            throw failure;
        });
        assertSame(failure, assertThrows(IOException.class, () -> BackgroundRequests.await(request)));
    }

    @Test
    void keepsTheOriginalExtractionException() {
        final ParsingException failure = new ParsingException("bad json");
        final Future<String> request = BackgroundRequests.start(() -> {
            throw failure;
        });
        assertSame(failure, assertThrows(ExtractionException.class, () -> BackgroundRequests.await(request)));
    }
}
