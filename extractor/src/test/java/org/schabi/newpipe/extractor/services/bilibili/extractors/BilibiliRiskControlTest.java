package org.schabi.newpipe.extractor.services.bilibili.extractors;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.services.bilibili.BilibiliService;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BilibiliRiskControlTest {

    private static Downloader answering(final String body, final AtomicInteger calls) {
        return new Downloader() {
            @Override
            public Response execute(@Nonnull final Request request) {
                calls.incrementAndGet();
                return new Response(200, "OK", Collections.emptyMap(), body, new byte[0], request.url());
            }

            @Override
            public CancellableCall executeAsync(@Nonnull final Request request,
                                                final AsyncCallback callback) {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test
    void rateLimitedSpaceApiThrowsTypedException() {
        final AtomicInteger calls = new AtomicInteger();
        final Downloader downloader = answering("{\"code\":-403,\"message\":\"访问权限不足\"}", calls);

        final BilibiliRiskControlException error = assertThrows(BilibiliRiskControlException.class,
                () -> BilibiliChannelExtractor.requestUserSpaceResponse(
                        downloader, "https://api.bilibili.com/x/space/acc/info?mid=1",
                        Collections.emptyMap()));

        assertEquals(-403, error.getCode());
        assertEquals(2, calls.get());
    }

    @Test
    void initialPageUsesTheApiThatFetchedItEvenAfterAnotherChannelRotatesTheMode()
            throws Exception {
        NewPipe.init(new Downloader() {
            @Override
            public Response execute(@Nonnull final Request request) {
                return new Response(200, "OK", Collections.singletonMap("date",
                        Collections.singletonList("Thu, 15 Oct 2026 12:00:00 GMT")),
                        bodyFor(request.url()), new byte[0], request.url());
            }

            @Override
            public CancellableCall executeAsync(@Nonnull final Request request,
                                                final AsyncCallback callback) {
                throw new UnsupportedOperationException();
            }
        });
        while (BilibiliService.getCurrentVideoApiMode() != BilibiliService.USER_VIDEO_API_MODE_WEB) {
            BilibiliService.rotateVideoApiMode();
        }
        final ChannelExtractor extractor = ServiceList.BiliBili
                .getChannelExtractor("https://space.bilibili.com/23630128");
        extractor.fetchPage();
        BilibiliService.rotateVideoApiMode();

        final List<StreamInfoItem> videos = extractor.getInitialPage().getItems();

        assertEquals(1, videos.size());
        assertEquals("Test video", videos.get(0).getName());
    }

    private static String bodyFor(final String url) {
        if (url.contains("/nav")) {
            return "{\"code\":0,\"data\":{\"wbi_img\":{"
                    + "\"img_url\":\"https://i0.hdslb.com/bfs/wbi/7cd084941338484aae1ad9425b84077c.png\","
                    + "\"sub_url\":\"https://i0.hdslb.com/bfs/wbi/4932caff0ff746eab6f01bf08b70ac45.png\"}}}";
        }
        if (url.contains("finger/spi")) {
            return "{\"code\":0,\"data\":{\"b_3\":\"buvid3_test\",\"b_4\":\"buvid4_test\"}}";
        }
        if (url.contains("GenWebTicket")) {
            return "{\"code\":0,\"data\":{\"ticket\":\"t\",\"created_at\":1700000000,\"ttl\":100000000}}";
        }
        if (url.contains("arc/search")) {
            return "{\"code\":0,\"data\":{\"list\":{\"vlist\":[{\"aid\":170001,"
                    + "\"bvid\":\"BV17x411w7KC\",\"title\":\"Test video\",\"author\":\"Tester\","
                    + "\"mid\":23630128,\"length\":\"01:00\",\"play\":1,\"created\":1700000000,"
                    + "\"pic\":\"https://i0.hdslb.com/test.jpg\"}]}}}";
        }
        if (url.contains("get_status_info_by_uids")) {
            return "{\"code\":0,\"data\":{\"23630128\":{\"live_status\":0}}}";
        }
        return "{\"code\":0,\"data\":{\"card\":{\"name\":\"Tester\"}}}";
    }
}
