package org.schabi.newpipe.extractor.services.bilibili.extractors;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItem;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.services.bilibili.WatchDataCache;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BilibiliBulletCommentsXmlDecodingTest {
    private static final String LIST_URL = "https://api.bilibili.com/x/v1/dm/list.so?oid=1";
    private static final Map<String, List<String>> DATE_HEADERS = Collections.singletonMap(
            "date", Collections.singletonList("Thu, 15 Oct 2026 12:00:00 GMT"));

    private static byte[] compress(final String text, final boolean nowrap) {
        final Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, nowrap);
        deflater.setInput(text.getBytes(StandardCharsets.UTF_8));
        deflater.finish();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final byte[] buf = new byte[1024];
        while (!deflater.finished()) {
            out.write(buf, 0, deflater.deflate(buf));
        }
        deflater.end();
        return out.toByteArray();
    }

    private static Response response(final int code, final byte[] bytes) {
        return new Response(code, "OK", DATE_HEADERS, "", bytes, LIST_URL);
    }

    private static Response textResponse(final int code, final String body) {
        return new Response(code, "OK", DATE_HEADERS, body, body.getBytes(StandardCharsets.UTF_8), LIST_URL);
    }

    private static void initBinaryDownloader(final byte[] payload) {
        NewPipe.init(new Downloader() {
            @Override
            public Response execute(@Nonnull final Request request) {
                final String url = request.url();
                if (url.contains("finger/spi")) {
                    return textResponse(200, "{\"code\":0,\"data\":{\"b_3\":\"buvid3_test\",\"b_4\":\"buvid4_test\"}}");
                }
                if (url.contains("GenWebTicket")) {
                    return textResponse(200, "{\"code\":0,\"data\":{\"ticket\":\"t\",\"created_at\":1700000000,\"ttl\":100000000}}");
                }
                return response(200, payload);
            }

            @Override
            public CancellableCall executeAsync(@Nonnull final Request request, final AsyncCallback callback) {
                throw new UnsupportedOperationException();
            }
        });
    }

    @Test
    void decodesPlainXmlAndZlibCompressedXml() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><i><state>0</state><d p=\"0.5,1,25,16777215,1,0,u,1\">你好</d></i>";
        final byte[] bomPrefixed = ("\uFEFF  " + xml).getBytes(StandardCharsets.UTF_8);
        assertEquals("你好", BilibiliBulletCommentsXmlDecoder.decode(response(200, bomPrefixed)).select("d").first().text());
        assertEquals("你好", BilibiliBulletCommentsXmlDecoder.decode(response(200, compress(xml, false))).select("d").first().text());
    }

    @Test
    void handlesEmptyTruncatedAndRiskControlResponsesSafely() {
        assertThrows(ParsingException.class, () -> BilibiliBulletCommentsXmlDecoder.decode(response(200, new byte[0])));
        final byte[] fullDeflate = compress("<?xml version=\"1.0\"?><i><d p=\"1,1,25,0,1,0,u,1\">hi</d></i>", true);
        final byte[] truncated = Arrays.copyOf(fullDeflate, Math.max(1, fullDeflate.length - 5));
        assertThrows(ParsingException.class, () -> BilibiliBulletCommentsXmlDecoder.decode(response(200, truncated)));
        assertThrows(ParsingException.class, () -> BilibiliBulletCommentsXmlDecoder.decode(textResponse(200, "{\"code\":-400,\"message\":\"bad\"}")));
        assertThrows(BilibiliRiskControlException.class, () -> BilibiliBulletCommentsXmlDecoder.decode(textResponse(200, "{\"code\":-352}")));
        assertThrows(BilibiliRiskControlException.class, () -> BilibiliBulletCommentsXmlDecoder.decode(textResponse(412, "<!DOCTYPE html><html></html>")));
    }

    @Test
    void skipsMalformedVotingAndScriptItemsWhilePreservingBracketTextAndMode7() throws Exception {
        final String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <i><state>0</state>
                  <d p="1.0,1,25">字段不足</d>
                  <d p="bad,1,25,16777215,1,0,u,1">非法时间</d>
                  <d p="2.0,1,25,16777215,1,3,u,1">投票弹幕</d>
                  <d p="2.5,8,25,16777215,1,0,u,1">console.log(1)</d>
                  <d p="3.0,9,25,16777215,1,0,u,1">def text t{}</d>
                  <d p="4.0,7,25,16777215,1,0,u,1">[0.1,0.2,"1-0",3,"高级弹幕文本"]</d>
                  <d p="5.0,1,25,notanumber,1,0,u,1">[星星]普通文本保持原样</d>
                </i>""";
        final WatchDataCache cache = new WatchDataCache();
        cache.setCid("BV1filterTest01?p=1", 4001L);
        initBinaryDownloader(compress(xml, true));

        final ListLinkHandler handler = ServiceList.BiliBili.getBulletCommentsLHFactory()
                .fromUrl("https://www.bilibili.com/video/BV1filterTest01");
        final BilibiliBulletCommentsExtractor extractor = new BilibiliBulletCommentsExtractor(ServiceList.BiliBili, handler, cache);
        extractor.fetchPage();
        final List<BulletCommentsInfoItem> items = extractor.getInitialPage().getItems();

        assertEquals(2, items.size());
        assertEquals("高级弹幕文本", items.get(0).getCommentText());
        assertEquals("[星星]普通文本保持原样", items.get(1).getCommentText());
        assertEquals(0xFFFFFFFF, items.get(1).getArgbColor());
        assertDoesNotThrow(extractor::disconnect);
    }

    @Test
    void returnsEmptyListWhenDanmakuClosed() throws Exception {
        final String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><i><state>1</state><d p=\"1.0,1,25,16777215,1,0,u,1\">ignored</d></i>";
        final WatchDataCache cache = new WatchDataCache();
        cache.setCid("BV1closedState1?p=1", 5001L);
        initBinaryDownloader(compress(xml, true));

        final ListLinkHandler handler = ServiceList.BiliBili.getBulletCommentsLHFactory()
                .fromUrl("https://www.bilibili.com/video/BV1closedState1");
        final BilibiliBulletCommentsExtractor extractor = new BilibiliBulletCommentsExtractor(ServiceList.BiliBili, handler, cache);
        extractor.fetchPage();
        assertTrue(extractor.getInitialPage().getItems().isEmpty());
    }
}
