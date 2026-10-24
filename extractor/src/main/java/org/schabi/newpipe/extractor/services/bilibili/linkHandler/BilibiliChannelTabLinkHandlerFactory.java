package org.schabi.newpipe.extractor.services.bilibili.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.util.Collections;
import java.util.List;

public final class BilibiliChannelTabLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final BilibiliChannelTabLinkHandlerFactory INSTANCE =
            new BilibiliChannelTabLinkHandlerFactory();

    private BilibiliChannelTabLinkHandlerFactory() {
    }

    public static BilibiliChannelTabLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(final String url) throws ParsingException {
        return BilibiliChannelLinkHandlerFactory.getInstance().getId(url);
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        return BilibiliChannelLinkHandlerFactory.getInstance().onAcceptUrl(url);
    }

    @Override
    public Filter getAvailableSortFilter() {
        return BilibiliChannelLinkHandlerFactory.getInstance().getAvailableSortFilter();
    }

    @Override
    public String getUrl(final String id, final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter) throws ParsingException {
        if (contentFilter != null && !contentFilter.isEmpty()) {
            final String tab = contentFilter.get(0).getName();
            if (ChannelTabs.PLAYLISTS.equals(tab)) {
                return "https://api.bilibili.com/x/polymer/web-space/seasons_series_list?mid="
                        + id + "&page_num=1&page_size=20";
            }
        }
        return BilibiliChannelLinkHandlerFactory.getInstance().getUrl(id, contentFilter, sortFilter);
    }

    @Override
    public ListLinkHandler fromUrl(final String url, final String baseUrl) throws ParsingException {
        if (url == null) {
            throw new IllegalArgumentException("url may not be null");
        }
        final String id = getId(url);
        final String tab = getTabFromUrl(url);
        final List<FilterItem> contentFilter = Collections.singletonList(
                new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, tab));
        final List<FilterItem> sortFilter = BilibiliChannelLinkHandlerFactory.getInstance().fromUrl(url).getSortFilter();
        final String cleanUrl = getUrl(id, contentFilter, sortFilter, baseUrl);
        return new ListLinkHandler(url, cleanUrl, id, contentFilter, sortFilter);
    }

    private static String getTabFromUrl(final String url) {
        if (url == null) {
            return ChannelTabs.VIDEOS;
        }
        if (url.contains("/channel/series")
                || url.contains("/channel/collection")
                || url.contains("/playlists")
                || url.contains("/x/polymer/web-space/seasons_series_list")
                || url.contains("tab=playlists")) {
            return ChannelTabs.PLAYLISTS;
        }
        return ChannelTabs.VIDEOS;
    }
}
