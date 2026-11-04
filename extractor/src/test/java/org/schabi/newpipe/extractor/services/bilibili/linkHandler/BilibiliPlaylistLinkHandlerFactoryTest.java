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
    void collectionMedialistUrlResolvesToSeason() throws ParsingException {
        final String url = "https://www.bilibili.com/medialist/play/23630128?business=space_collection&business_id=2216020";

        assertTrue(factory.onAcceptUrl(url));
        final ListLinkHandler handler = factory.fromUrl(url);
        final BilibiliPlaylistLinkHandlerFactory.PlaylistTarget target =
                BilibiliPlaylistLinkHandlerFactory.parseTarget(handler.getUrl());
        assertEquals("seasons_archives", target.type);
        assertEquals("23630128", target.mid);
        assertEquals("2216020", target.id);
    }

    @Test
    void seriesMedialistUrlResolvesToSeries() throws ParsingException {
        final String url = "https://www.bilibili.com/medialist/play/23630128?business=space_series&business_id=340933";

        assertTrue(factory.onAcceptUrl(url));
        final BilibiliPlaylistLinkHandlerFactory.PlaylistTarget target =
                BilibiliPlaylistLinkHandlerFactory.parseTarget(url);
        assertEquals("series", target.type);
        assertEquals("23630128", target.mid);
        assertEquals("340933", target.id);
    }

    @Test
    void unsupportedMedialistBusinessIsRejected() throws ParsingException {
        assertFalse(factory.onAcceptUrl("https://www.bilibili.com/medialist/play/23630128?business=other&business_id=1"));
    }
}
