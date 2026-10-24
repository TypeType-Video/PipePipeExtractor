package org.schabi.newpipe.extractor.services.bilibili.extractors;

import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.getDefaultCookies;
import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.getHeaders;
import static org.schabi.newpipe.extractor.services.bilibili.utils.getNextPageFromCurrentUrl;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.MultiInfoItemsCollector;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelTabExtractor;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;

import java.io.IOException;

import javax.annotation.Nonnull;

public class BilibiliChannelTabExtractor extends ChannelTabExtractor {
    public BilibiliChannelTabExtractor(final StreamingService service, final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) throws IOException, ExtractionException {
    }

    private boolean isVideosTab() {
        return !getLinkHandler().getContentFilters().isEmpty()
                && ChannelTabs.VIDEOS.equals(getLinkHandler().getContentFilters().get(0).getName());
    }

    private boolean isPlaylistsTab() {
        return !getLinkHandler().getContentFilters().isEmpty()
                && ChannelTabs.PLAYLISTS.equals(getLinkHandler().getContentFilters().get(0).getName());
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        if (isVideosTab()) {
            final BilibiliChannelExtractor extractor = new BilibiliChannelExtractor(getService(), getLinkHandler());
            extractor.onFetchPage(getDownloader());
            return (InfoItemsPage<InfoItem>) (InfoItemsPage<?>) extractor.getInitialPage();
        }
        return getPage(new Page(getLinkHandler().getUrl(), getDefaultCookies()));
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page) throws IOException, ExtractionException {
        if (isVideosTab()) {
            final BilibiliChannelExtractor extractor = new BilibiliChannelExtractor(getService(), getLinkHandler());
            return (InfoItemsPage<InfoItem>) (InfoItemsPage<?>) extractor.getPage(page);
        }
        if (!isPlaylistsTab()) {
            throw new ParsingException("Unsupported bilibili channel tab: " + getTab());
        }

        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());
        final String referer = "https://space.bilibili.com/" + getId();
        final String response = getDownloader().get(page.getUrl(), getHeaders(referer)).responseBody();
        try {
            final JsonObject root = JsonParser.object().from(response);
            if (root.getInt("code", 0) != 0 || !root.has("data") || root.getObject("data") == null) {
                return InfoItemsPage.emptyPage();
            }
            final JsonObject data = root.getObject("data");
            if (!data.has("items_lists") || data.getObject("items_lists") == null) {
                return InfoItemsPage.emptyPage();
            }

            final JsonObject itemsLists = data.getObject("items_lists");
            final JsonArray seasonsList = itemsLists.getArray("seasons_list");
            final JsonArray seriesList = itemsLists.getArray("series_list");

            final int seasonsSize = seasonsList != null ? seasonsList.size() : 0;
            final int seriesSize = seriesList != null ? seriesList.size() : 0;

            if (seasonsSize + seriesSize == 0) {
                return new InfoItemsPage<>(collector, null);
            }

            if (seasonsList != null) {
                for (int i = 0; i < seasonsSize; i++) {
                    final BilibiliPlaylistInfoItemExtractor extractor = new BilibiliPlaylistInfoItemExtractor(
                            seasonsList.getObject(i), "seasons_archives");
                    if (extractor.getStreamCount() == 0) {
                        continue;
                    }
                    collector.commit(extractor);
                }
            }

            if (seriesList != null) {
                for (int i = 0; i < seriesSize; i++) {
                    final BilibiliPlaylistInfoItemExtractor extractor = new BilibiliPlaylistInfoItemExtractor(
                            seriesList.getObject(i), "archives");
                    if (extractor.getStreamCount() == 0) {
                        continue;
                    }
                    collector.commit(extractor);
                }
            }

            Page nextPage = null;
            if (itemsLists.has("page") && itemsLists.getObject("page") != null) {
                final JsonObject pageObj = itemsLists.getObject("page");
                final int pageNum = pageObj.getInt("page_num", 1);
                final int pageSize = pageObj.getInt("page_size", 20);
                final int total = pageObj.getInt("total", 0);
                if ((long) pageNum * pageSize < total && (seasonsSize + seriesSize) > 0) {
                    nextPage = new Page(getNextPageFromCurrentUrl(page.getUrl(), "page_num", 1), getDefaultCookies());
                }
            }

            if (ServiceList.BiliBili.getFilterTypes().contains("channels")) {
                collector.applyBlocking(ServiceList.BiliBili.getFilterConfig());
            }

            return new InfoItemsPage<>(collector, nextPage);
        } catch (final JsonParserException e) {
            throw new ParsingException("Could not parse Bilibili seasons/series response", e);
        }
    }
}