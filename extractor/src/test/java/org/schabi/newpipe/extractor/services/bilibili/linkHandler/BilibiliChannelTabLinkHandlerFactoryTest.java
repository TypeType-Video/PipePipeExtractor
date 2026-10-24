package org.schabi.newpipe.extractor.services.bilibili.linkHandler;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class BilibiliChannelTabLinkHandlerFactoryTest {

    private final BilibiliChannelTabLinkHandlerFactory factory =
            BilibiliChannelTabLinkHandlerFactory.getInstance();

    @Test
    void testAcceptsChannelUrls() throws ParsingException {
        assertTrue(factory.onAcceptUrl("https://space.bilibili.com/23630128"));
        assertTrue(factory.onAcceptUrl("https://space.bilibili.com/23630128/channel/series"));
        assertTrue(factory.onAcceptUrl("https://space.bilibili.com/23630128/channel/collection"));
        assertTrue(factory.onAcceptUrl("https://space.bilibili.com/23630128/playlists"));
        assertTrue(factory.onAcceptUrl("https://space.bilibili.com/23630128/video"));
        assertTrue(factory.onAcceptUrl("23630128"));
        assertFalse(factory.onAcceptUrl("https://www.youtube.com/channel/UC123"));
    }

    @Test
    void testGetId() throws ParsingException {
        assertEquals("23630128", factory.getId("https://space.bilibili.com/23630128"));
        assertEquals("23630128", factory.getId("https://space.bilibili.com/23630128/channel/series"));
        assertEquals("23630128", factory.getId("https://space.bilibili.com/23630128/playlists"));
        assertEquals("23630128", factory.getId("23630128"));
    }

    @Test
    void testFromUrlPlaylistsTab() throws ParsingException {
        ListLinkHandler seriesHandler = factory.fromUrl("https://space.bilibili.com/23630128/channel/series");
        assertEquals("23630128", seriesHandler.getId());
        assertEquals(ChannelTabs.PLAYLISTS, seriesHandler.getContentFilters().get(0).getName());
        assertEquals("https://api.bilibili.com/x/polymer/web-space/seasons_series_list?mid=23630128&page_num=1&page_size=20",
                seriesHandler.getUrl());

        ListLinkHandler playlistsHandler = factory.fromUrl("https://space.bilibili.com/23630128/playlists");
        assertEquals("23630128", playlistsHandler.getId());
        assertEquals(ChannelTabs.PLAYLISTS, playlistsHandler.getContentFilters().get(0).getName());
        assertEquals("https://api.bilibili.com/x/polymer/web-space/seasons_series_list?mid=23630128&page_num=1&page_size=20",
                playlistsHandler.getUrl());
    }

    @Test
    void testFromUrlVideosTab() throws ParsingException {
        ListLinkHandler defaultHandler = factory.fromUrl("https://space.bilibili.com/23630128");
        assertEquals("23630128", defaultHandler.getId());
        assertEquals(ChannelTabs.VIDEOS, defaultHandler.getContentFilters().get(0).getName());
        assertEquals("https://space.bilibili.com/23630128", defaultHandler.getUrl());
    }

    @Test
    void testFromQueryPlaylists() throws ParsingException {
        ListLinkHandler handler = factory.fromQuery("23630128",
                Collections.singletonList(new FilterItem(0, ChannelTabs.PLAYLISTS)), null);
        assertEquals("23630128", handler.getId());
        assertEquals(ChannelTabs.PLAYLISTS, handler.getContentFilters().get(0).getName());
        assertEquals("https://api.bilibili.com/x/polymer/web-space/seasons_series_list?mid=23630128&page_num=1&page_size=20",
                handler.getUrl());
    }
}