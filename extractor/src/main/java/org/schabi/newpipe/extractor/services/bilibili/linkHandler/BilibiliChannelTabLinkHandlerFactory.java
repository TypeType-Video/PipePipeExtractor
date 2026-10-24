package org.schabi.newpipe.extractor.services.bilibili.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        if (url == null) {
            throw new ParsingException("Not a bilibili channel link: empty URL.");
        }
        final String trimmedUrl = url.trim();
        if (trimmedUrl.matches("^\\d+$")) {
            return trimmedUrl;
        }
        final Matcher spaceId = Pattern.compile(
                "^https?://space\\.bilibili\\.com/(\\d+)(?:/|\\?|$)", Pattern.CASE_INSENSITIVE)
                .matcher(trimmedUrl);
        if (spaceId.find()) {
            return spaceId.group(1);
        }
        final Matcher apiUrl = Pattern.compile(
                "^https?://api\\.bilibili\\.com/x/polymer/web-space/seasons_series_list(?:\\?.*)?$",
                Pattern.CASE_INSENSITIVE).matcher(trimmedUrl);
        if (apiUrl.matches()) {
            final Matcher apiMid = Pattern.compile("[?&]mid=(\\d+)(?:&|$)").matcher(trimmedUrl);
            if (apiMid.find()) {
                return apiMid.group(1);
            }
        }
        throw new ParsingException("Not a bilibili channel link: " + url);
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        try {
            getId(url);
            return true;
        } catch (final ParsingException e) {
            return false;
        }
    }

    @Override
    public Filter getAvailableSortFilter() {
        return new BilibiliChannelLinkHandlerFactory().getAvailableSortFilter();
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
        return new BilibiliChannelLinkHandlerFactory().getUrl(id, contentFilter, sortFilter);
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
        final List<FilterItem> sortFilter = new BilibiliChannelLinkHandlerFactory().fromUrl(url).getSortFilter();
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
