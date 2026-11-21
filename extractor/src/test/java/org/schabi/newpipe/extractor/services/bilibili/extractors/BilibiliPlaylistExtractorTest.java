package org.schabi.newpipe.extractor.services.bilibili.extractors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliPlaylistLinkHandlerFactory;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import javax.annotation.Nonnull;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class BilibiliPlaylistExtractorTest {

    @BeforeAll
    static void setUp() {
        NewPipe.init(new Downloader() {
            @Override
            public Response execute(@Nonnull Request request) {
                if (request.url().contains("finger/spi")) {
                    return new Response(200, "OK",
                            Collections.singletonMap("date", Collections.singletonList("Thu, 15 Oct 2026 12:00:00 GMT")),
                            "{\"code\":0,\"data\":{\"b_3\":\"buvid3_test\",\"b_4\":\"buvid4_test\"}}",
                            new byte[0], request.url());
                }
                if (request.url().contains("GenWebTicket")) {
                    return new Response(200, "OK", Collections.emptyMap(),
                            "{\"code\":0,\"data\":{\"ticket\":\"ticket_test\",\"created_at\":1700000000,\"ttl\":100000000}}",
                            new byte[0], request.url());
                }
                return new Response(200, "OK", Collections.emptyMap(), "{\"code\":0,\"data\":{}}", new byte[0], request.url());
            }

            @Override
            public CancellableCall executeAsync(@Nonnull Request request, AsyncCallback callback) {
                return null;
            }
        });
    }

    @Test
    void getPageWithoutOnFetchPageResolvesMidUploaderAndExactTotalBoundary() throws Exception {
        StringBuilder archivesBuilder = new StringBuilder("[");
        for (int i = 0; i < 30; i++) {
            if (i > 0) archivesBuilder.append(",");
            archivesBuilder.append(String.format(
                    "{\"aid\":%d,\"bvid\":\"BV1test%d\",\"title\":\"Video %d\",\"pic\":\"http://i0.hdslb.com/bfs/archive/v%d.jpg\",\"duration\":120}",
                    100 + i, i, i, i));
        }
        archivesBuilder.append("]");
        String archivesJson = archivesBuilder.toString();

        try {
            NewPipe.init(new Downloader() {
                @Override
                public Response execute(@Nonnull Request request) {
                    if (request.url().contains("seasons_archives_list")) {
                        long total = request.url().contains("season_id=649231") ? 60 : 65;
                        String body = String.format(
                                "{\"code\":0,\"data\":{\"meta\":{\"season_id\":649231,\"mid\":11545648,\"name\":\"Test\"},\"archives\":%s,\"page\":{\"page_num\":2,\"page_size\":30,\"total\":%d}}}",
                                archivesJson, total);
                        return new Response(200, "OK", Collections.emptyMap(), body, new byte[0], request.url());
                    }
                    if (request.url().contains("x/web-interface/card") && request.url().contains("mid=11545648")) {
                        String body = "{\"code\":0,\"data\":{\"card\":{\"name\":\"TestUploader\",\"face\":\"http://i0.hdslb.com/bfs/face/avatar.jpg\"}}}";
                        return new Response(200, "OK", Collections.emptyMap(), body, new byte[0], request.url());
                    }
                    if (request.url().contains("finger/spi")) {
                        return new Response(200, "OK",
                                Collections.singletonMap("date", Collections.singletonList("Thu, 15 Oct 2026 12:00:00 GMT")),
                                "{\"code\":0,\"data\":{\"b_3\":\"buvid3_test\",\"b_4\":\"buvid4_test\"}}",
                                new byte[0], request.url());
                    }
                    if (request.url().contains("GenWebTicket")) {
                        return new Response(200, "OK", Collections.emptyMap(),
                                "{\"code\":0,\"data\":{\"ticket\":\"ticket_test\",\"created_at\":1700000000,\"ttl\":100000000}}",
                                new byte[0], request.url());
                    }
                    return new Response(200, "OK", Collections.emptyMap(), "{\"code\":0,\"data\":{}}", new byte[0], request.url());
                }

                @Override
                public CancellableCall executeAsync(@Nonnull Request request, AsyncCallback callback) {
                    return null;
                }
            });

            BilibiliPlaylistLinkHandlerFactory factory = new BilibiliPlaylistLinkHandlerFactory();

            // Case 1: total == 60 (exact multiple of 30) on page 2 -> no next page, uploader resolved via meta.mid
            ListLinkHandler handler60 = factory.fromUrl("season:649231");
            BilibiliPlaylistExtractor extractor60 = new BilibiliPlaylistExtractor(ServiceList.BiliBili, handler60);
            Page page2Of60 = new Page("https://api.bilibili.com/x/polymer/web-space/seasons_archives_list?season_id=649231&sort_reverse=false&page_num=2&page_size=30");
            ListExtractor.InfoItemsPage<StreamInfoItem> result60 = extractor60.getPage(page2Of60);

            assertEquals(30, result60.getItems().size());
            assertEquals("TestUploader", result60.getItems().get(0).getUploaderName());
            assertNull(result60.getNextPage());

            // Case 2: total == 65 on page 2 -> page_num=3 generated
            ListLinkHandler handler65 = factory.fromUrl("season:999999");
            BilibiliPlaylistExtractor extractor65 = new BilibiliPlaylistExtractor(ServiceList.BiliBili, handler65);
            Page page2Of65 = new Page("https://api.bilibili.com/x/polymer/web-space/seasons_archives_list?season_id=999999&sort_reverse=false&page_num=2&page_size=30");
            ListExtractor.InfoItemsPage<StreamInfoItem> result65 = extractor65.getPage(page2Of65);

            assertEquals(30, result65.getItems().size());
            assertNotNull(result65.getNextPage());
            assertTrue(result65.getNextPage().getUrl().contains("page_num=3"));
        } finally {
            setUp();
        }
    }
}
