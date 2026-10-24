package org.schabi.newpipe.extractor.services.bilibili.linkHandler;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BilibiliPlaylistLinkHandlerFactoryTest {
    private final BilibiliPlaylistLinkHandlerFactory factory = new BilibiliPlaylistLinkHandlerFactory();

    @Test
    void collectionMedialistUrlResolvesToSeasonApi() throws ParsingException {
        final String url = "https://www.bilibili.com/medialist/play/23630128?business=space_collection&business_id=2216020";

        assertTrue(factory.onAcceptUrl(url));
        final ListLinkHandler handler = factory.fromUrl(url);
        assertEquals(url, handler.getUrl());
        assertEquals("https://api.bilibili.com/x/polymer/web-space/seasons_archives_list?mid=23630128&season_id=2216020&sort_reverse=false&page_num=1&page_size=30",
                BilibiliPlaylistLinkHandlerFactory.toApiUrl(handler.getUrl()));
    }

    @Test
    void seriesMedialistUrlResolvesToSeriesApi() throws ParsingException {
        final String url = "https://www.bilibili.com/medialist/play/23630128?business=space_series&business_id=340933";

        assertTrue(factory.onAcceptUrl(url));
        assertEquals("https://api.bilibili.com/x/series/archives?mid=23630128&series_id=340933&only_normal=true&sort=desc&pn=1&ps=30",
                BilibiliPlaylistLinkHandlerFactory.toApiUrl(url));
    }

    @Test
    void unsupportedMedialistBusinessIsRejected() throws ParsingException {
        assertFalse(factory.onAcceptUrl("https://www.bilibili.com/medialist/play/23630128?business=other&business_id=1"));
    }
}
