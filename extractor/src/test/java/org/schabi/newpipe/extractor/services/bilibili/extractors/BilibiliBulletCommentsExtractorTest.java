package org.schabi.newpipe.extractor.services.bilibili.extractors;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItem;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.services.bilibili.WatchDataCache;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.*;

class BilibiliBulletCommentsExtractorTest {
    private static final Map<String, List<String>> DATE_HEADERS =
            Map.of("date", List.of("Thu, 15 Oct 2026 12:00:00 GMT"));

    private static byte[] compressRawDeflate(final String text) {
        final Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        deflater.setInput(text.getBytes(StandardCharsets.UTF_8));
        deflater.finish();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buf = new byte[1024];
        while (!deflater.finished()) out.write(buf, 0, deflater.deflate(buf));
        deflater.end();
        return out.toByteArray();
    }

    private static Downloader mockDownloader(final Function<Request, Response> handler) {
        return new Downloader() {
            @Override
            public Response execute(@Nonnull final Request request) {
                final String url = request.url();
                if (url.contains("finger/spi")) return textResponse(url, 200, "{\"code\":0,\"data\":{\"b_3\":\"buvid3_test\",\"b_4\":\"buvid4_test\"}}");
                if (url.contains("GenWebTicket")) return textResponse(url, 200, "{\"code\":0,\"data\":{\"ticket\":\"t\",\"created_at\":1700000000,\"ttl\":100000000}}");
                return handler.apply(request);
            }

            @Override
            public CancellableCall executeAsync(@Nonnull final Request request, final AsyncCallback callback) {
                throw new UnsupportedOperationException();
            }
        };
    }

    private static Response textResponse(final String url, final int code, final String body) {
        return new Response(code, "OK", DATE_HEADERS, body, body.getBytes(StandardCharsets.UTF_8), url);
    }

    private static Response binaryResponse(final String url, final byte[] bytes) {
        return new Response(200, "OK", DATE_HEADERS, "", bytes, url);
    }

    private static BilibiliBulletCommentsExtractor createExtractor(final String url, final WatchDataCache cache) throws Exception {
        return new BilibiliBulletCommentsExtractor(ServiceList.BiliBili, ServiceList.BiliBili.getBulletCommentsLHFactory().fromUrl(url), cache);
    }

    @Test
    void extractsRawDeflateUtf8ChineseDanmakuWithSyncOffset() throws Exception {
        final String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <i><state>0</state>
                  <d p="1.500,1,25,16777215,1700000000,0,abc,1,10">前方高能！中文弹幕测试</d>
                  <d p="2.250,5,36,16711680,1700000001,0,def,2,10">顶部大字</d>
                  <d p="3.000,4,18,255,1700000002,0,ghi,3,10">底部小字</d>
                </i>""";
        final byte[] deflated = compressRawDeflate(xml);
        final WatchDataCache cache = new WatchDataCache();
        final List<String> seenUrls = new ArrayList<>();

        NewPipe.init(mockDownloader(request -> {
            seenUrls.add(request.url());
            if (request.url().contains("wbi/view?bvid=BV1rawDeflate1")) {
                return textResponse(request.url(), 200, "{\"code\":0,\"data\":{\"cid\":1001,\"pages\":[{\"page\":1,\"cid\":1001}]}}");
            }
            if (request.url().contains("dm/list.so?oid=1001")) {
                assertTrue(request.headers().containsKey("Referer"));
                assertTrue(request.headers().containsKey("User-Agent"));
                return binaryResponse(request.url(), deflated);
            }
            throw new AssertionError("Unexpected URL: " + request.url());
        }));

        final BilibiliBulletCommentsExtractor extractor = createExtractor("https://www.bilibili.com/video/BV1rawDeflate1", cache);
        extractor.fetchPage();
        final List<BulletCommentsInfoItem> items = extractor.getInitialPage().getItems();

        assertEquals(3, items.size());
        assertFalse(seenUrls.stream().anyMatch(u -> u.contains("playurl")));
        assertEquals("前方高能！中文弹幕测试", items.get(0).getCommentText());
        assertEquals(0xFFFFFFFF, items.get(0).getArgbColor());
        assertEquals(BulletCommentsInfoItem.Position.REGULAR, items.get(0).getPosition());
        assertEquals(0.6, items.get(0).getRelativeFontSize(), 1e-6);
        assertEquals(Duration.ofMillis(4000), items.get(0).getDuration());
        assertEquals(BulletCommentsInfoItem.Position.TOP, items.get(1).getPosition());
        assertEquals(0.7, items.get(1).getRelativeFontSize(), 1e-6);
        assertEquals(Duration.ofMillis(4750), items.get(1).getDuration());
        assertEquals(BulletCommentsInfoItem.Position.BOTTOM, items.get(2).getPosition());
        assertEquals(0.5, items.get(2).getRelativeFontSize(), 1e-6);
        assertEquals(Duration.ofMillis(5500), items.get(2).getDuration());
    }

    @Test
    void resolvesMultiPartCidAndReusesWatchDataCache() throws Exception {
        final byte[] deflated = compressRawDeflate("<?xml version=\"1.0\" encoding=\"UTF-8\"?><i><state>0</state><d p=\"4.0,1,25,16777215,1,0,u,1\">第二P弹幕</d></i>");
        final WatchDataCache cache = new WatchDataCache();
        final AtomicInteger viewCalls = new AtomicInteger();

        NewPipe.init(mockDownloader(request -> {
            if (request.url().contains("wbi/view?bvid=BV1multiPart02")) {
                viewCalls.incrementAndGet();
                return textResponse(request.url(), 200, "{\"code\":0,\"data\":{\"cid\":2001,\"pages\":[{\"page\":1,\"cid\":2001},{\"page\":2,\"cid\":2002}]}}");
            }
            if (request.url().contains("dm/list.so?oid=2002")) {
                return binaryResponse(request.url(), deflated);
            }
            throw new AssertionError("Unexpected URL: " + request.url());
        }));

        final BilibiliBulletCommentsExtractor first = createExtractor("https://www.bilibili.com/video/BV1multiPart02?p=2", cache);
        first.fetchPage();
        assertEquals(1, viewCalls.get());
        assertEquals(2002L, cache.getCid("BV1multiPart02?p=2"));
        assertEquals("第二P弹幕", first.getInitialPage().getItems().get(0).getCommentText());

        final BilibiliBulletCommentsExtractor second = createExtractor("https://www.bilibili.com/video/BV1multiPart02?p=2", cache);
        second.fetchPage();
        assertEquals(1, viewCalls.get());
    }

    @Test
    void resolvesBangumiCidAndSurfacesMetadataRiskControl() throws Exception {
        final byte[] deflated = compressRawDeflate("<?xml version=\"1.0\" encoding=\"UTF-8\"?><i><state>0</state><d p=\"1.0,1,25,16777215,1,0,u,1\">番剧弹幕</d></i>");
        final WatchDataCache cache = new WatchDataCache();

        NewPipe.init(mockDownloader(request -> {
            if (request.url().contains("pgc/view/web/season?ep_id=98765")) {
                return textResponse(request.url(), 200, """
                        {"code":0,"result":{"episodes":[
                          {"id":98764,"cid":3001,"bvid":"BV1ep1","share_url":"https://www.bilibili.com/bangumi/play/ep98764"},
                          {"id":98765,"cid":3002,"bvid":"BV1ep2","share_url":"https://www.bilibili.com/bangumi/play/ep98765"}
                        ]}}""");
            }
            if (request.url().contains("dm/list.so?oid=3002")) {
                return binaryResponse(request.url(), deflated);
            }
            if (request.url().contains("pgc/view/web/season?season_id=54321")) {
                return textResponse(request.url(), 412, "<!DOCTYPE html><html></html>");
            }
            throw new AssertionError("Unexpected URL: " + request.url());
        }));

        final BilibiliBulletCommentsExtractor epExtractor = createExtractor("https://www.bilibili.com/bangumi/play/ep98765", cache);
        epExtractor.fetchPage();
        assertEquals(3002L, cache.getCid("ep98765"));
        assertEquals("番剧弹幕", epExtractor.getInitialPage().getItems().get(0).getCommentText());
        assertThrows(BilibiliRiskControlException.class, createExtractor("https://www.bilibili.com/bangumi/play/ss54321", cache)::fetchPage);
    }

    @Test
    void streamExtractorWiresPerIdLiveRoomAndStartTimeInCache() throws Exception {
        final WatchDataCache cache = new WatchDataCache();
        NewPipe.init(mockDownloader(request -> {
            if (request.url().contains("room_init?id=77")) {
                return textResponse(request.url(), 200, "{\"code\":0,\"data\":{\"room_id\":999001}}");
            }
            if (request.url().contains("getRoomBaseInfo")) {
                return textResponse(request.url(), 200, "{\"code\":0,\"data\":{\"by_room_ids\":{\"999001\":{\"room_id\":999001,\"live_status\":1,\"live_time\":\"2026-11-20 12:00:00\"}}}}");
            }
            return textResponse(request.url(), 200, "{\"code\":0,\"data\":{\"playurl_info\":{\"playurl\":{\"stream\":[]}}}}");
        }));

        final LinkHandler linkHandler = ServiceList.BiliBili.getStreamLHFactory().fromUrl("https://live.bilibili.com/77");
        new BillibiliStreamExtractor(ServiceList.BiliBili, linkHandler, cache).fetchPage();
        final long expectedStart = LocalDateTime.of(2026, 11, 20, 12, 0, 0).toEpochSecond(ZoneOffset.ofHours(8));
        assertEquals(999001L, cache.findRoomId("77"));
        assertEquals(expectedStart, cache.findStartTime("77"));
    }
}
