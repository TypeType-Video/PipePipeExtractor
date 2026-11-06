package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonObject;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NiconicoWatchDataCacheTest {
    private static final String WATCH = "{\"client\":{\"watchTrackId\":\"t_1\"},\"video\":{\"title\":\"%s\"},"
            + "\"media\":{\"domand\":{\"accessRightKey\":\"k\",\"videos\":[{\"id\":\"video-h264-360p\","
            + "\"isAvailable\":true}],\"audios\":[]}},\"okReason\":\"PURELY\"}";

    private static final class Pages extends Downloader {
        private final int guestCode;
        private final String guestBody;
        final List<String> asked = new ArrayList<>();

        Pages(final int guestCode, final String guestBody) {
            this.guestCode = guestCode;
            this.guestBody = guestBody;
        }

        @Override
        public Response execute(@Nonnull final Request request) {
            asked.add(request.url());
            if (request.url().contains("/api/watch/v3_guest/")) {
                return new Response(guestCode, "", Collections.emptyMap(), guestBody, guestBody.getBytes(),
                        request.url());
            }
            final String data = "{\"data\":{\"response\":" + String.format(WATCH, "page") + "}}";
            final String body = "<html><head><meta name=\"server-response\" content=\""
                    + data.replace("\"", "&quot;") + "\"></head></html>";
            return new Response(200, "", Collections.emptyMap(), body, body.getBytes(), request.url());
        }

        @Override
        public CancellableCall executeAsync(@Nonnull final Request request, final AsyncCallback callback) {
            throw new UnsupportedOperationException();
        }
    }

    @Test
    void readsTheGuestWatchApiWithoutTheWatchPage() throws Exception {
        final Pages pages = new Pages(200,
                "{\"meta\":{\"status\":200},\"data\":" + String.format(WATCH, "api") + "}");

        final JsonObject watch = new NiconicoWatchDataCache().refreshAndGetWatchData(pages, "sm9");

        assertEquals("api", watch.getObject("video").getString("title"));
        assertEquals(1, pages.asked.size());
        assertTrue(pages.asked.get(0).startsWith("https://www.nicovideo.jp/api/watch/v3_guest/sm9?actionTrackId="));
    }

    @Test
    void fallsBackToTheWatchPageWhenTheGuestApiRefuses() throws Exception {
        final Pages pages = new Pages(403, "{\"meta\":{\"status\":403},\"data\":{\"reasonCode\":\"X\"}}");

        final JsonObject watch = new NiconicoWatchDataCache().refreshAndGetWatchData(pages, "sm9");

        assertEquals("page", watch.getObject("video").getString("title"));
        assertEquals(2, pages.asked.size());
    }
}
