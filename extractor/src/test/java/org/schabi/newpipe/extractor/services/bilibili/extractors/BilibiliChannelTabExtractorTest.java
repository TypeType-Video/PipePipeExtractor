package org.schabi.newpipe.extractor.services.bilibili.extractors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliChannelTabLinkHandlerFactory;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BilibiliChannelTabExtractorTest {

    private static final String MOCK_SEASONS_SERIES_JSON = "{"
            + "\"code\":0,\"message\":\"0\",\"ttl\":1,\"data\":{"
            + "\"items_lists\":{"
            + "\"page\":{\"page_num\":1,\"page_size\":20,\"total\":2},"
            + "\"seasons_list\":[{"
            + "  \"meta\":{\"category\":0,\"cover\":\"https://archive.biliimg.com/cover1.png\","
            + "    \"description\":\"\",\"mid\":23630128,\"name\":\"合集·试卷讲解\","
            + "    \"ptime\":1765252800,\"season_id\":2216020,\"total\":73,\"title\":\"试卷讲解\"}"
            + "}],"
            + "\"series_list\":[{"
            + "  \"meta\":{\"category\":1,\"cover\":\"http://i1.hdslb.com/cover2.jpg\","
            + "    \"creator\":\"\",\"description\":\"\",\"mid\":23630128,\"name\":\"一轮复习\","
            + "    \"series_id\":340933,\"total\":13}"
            + "}]"
            + "}"
            + "}}";
    private static String downloaderResponse = MOCK_SEASONS_SERIES_JSON;

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
                return new Response(200, "OK", Collections.emptyMap(),
                        downloaderResponse, new byte[0], request.url());
            }

            @Override
            public CancellableCall executeAsync(@Nonnull Request request, AsyncCallback callback) {
                return null;
            }
        });
    }

    @Test
    void testExtractPlaylistsTab() throws Exception {
        ListLinkHandler handler = BilibiliChannelTabLinkHandlerFactory.getInstance().fromQuery("23630128",
                Collections.singletonList(new FilterItem(0, ChannelTabs.PLAYLISTS)), null);

        BilibiliChannelTabExtractor extractor = new BilibiliChannelTabExtractor(ServiceList.BiliBili, handler);
        extractor.onFetchPage(NewPipe.getDownloader());

        ListExtractor.InfoItemsPage<InfoItem> initialPage = extractor.getInitialPage();
        List<InfoItem> items = initialPage.getItems();
        assertEquals(2, items.size());

        // First item: season
        assertTrue(items.get(0) instanceof PlaylistInfoItem);
        PlaylistInfoItem season = (PlaylistInfoItem) items.get(0);
        assertEquals("合集·试卷讲解", season.getName());
        assertEquals("https://www.bilibili.com/medialist/play/23630128?business=space_collection&business_id=2216020", season.getUrl());
        assertEquals("https://archive.biliimg.com/cover1.png", season.getThumbnailUrl());
        assertEquals(73, season.getStreamCount());
        assertEquals("23630128", season.getUploaderId());

        // Second item: series
        assertTrue(items.get(1) instanceof PlaylistInfoItem);
        PlaylistInfoItem series = (PlaylistInfoItem) items.get(1);
        assertEquals("一轮复习", series.getName());
        assertEquals("https://www.bilibili.com/medialist/play/23630128?business=space_series&business_id=340933", series.getUrl());
        assertEquals("https://i1.hdslb.com/cover2.jpg", series.getThumbnailUrl());
        assertEquals(13, series.getStreamCount());
        assertEquals("23630128", series.getUploaderId());

        // total == 2, page_size == 20 -> no next page
        assertNull(initialPage.getNextPage());
    }

    @Test
    void testApiErrorIsNotReportedAsEmptyPlaylistList() throws Exception {
        downloaderResponse = "{\"code\":-352,\"message\":\"risk control\",\"data\":null}";
        try {
            final BilibiliChannelTabExtractor extractor = createPlaylistExtractor();
            assertThrows(org.schabi.newpipe.extractor.exceptions.ParsingException.class,
                    extractor::getInitialPage);
        } finally {
            downloaderResponse = MOCK_SEASONS_SERIES_JSON;
        }
    }

    @Test
    void testSuccessfulEmptyListsReturnEmptyPage() throws Exception {
        downloaderResponse = "{\"code\":0,\"data\":{\"items_lists\":{"
                + "\"seasons_list\":[],\"series_list\":[],"
                + "\"page\":{\"page_num\":1,\"page_size\":20,\"total\":0}}}}";
        try {
            final ListExtractor.InfoItemsPage<InfoItem> page = createPlaylistExtractor().getInitialPage();
            assertTrue(page.getItems().isEmpty());
            assertNull(page.getNextPage());
        } finally {
            downloaderResponse = MOCK_SEASONS_SERIES_JSON;
        }
    }

    private BilibiliChannelTabExtractor createPlaylistExtractor() throws Exception {
        final ListLinkHandler handler = BilibiliChannelTabLinkHandlerFactory.getInstance().fromQuery("23630128",
                Collections.singletonList(new FilterItem(0, ChannelTabs.PLAYLISTS)), null);
        return new BilibiliChannelTabExtractor(ServiceList.BiliBili, handler);
    }
}
