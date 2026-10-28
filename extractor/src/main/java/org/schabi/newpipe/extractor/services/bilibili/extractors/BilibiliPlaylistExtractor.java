package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.schabi.newpipe.extractor.InfoItemsCollector;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliChannelLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliPlaylistLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.bilibili.utils;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.annotation.Nonnull;

import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.*;

public class BilibiliPlaylistExtractor extends PlaylistExtractor {
    public JsonObject data;
    public String type;
    private String firstPageApiUrl = "";
    private JsonObject userData;

    public BilibiliPlaylistExtractor(StreamingService service, ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {
        try {
            final String playlistUrl = getLinkHandler().getUrl();
            final String resolvedApiUrl = BilibiliPlaylistLinkHandlerFactory.toApiUrl(playlistUrl);
            firstPageApiUrl = resolvedApiUrl != null ? resolvedApiUrl : playlistUrl;
            data = JsonParser.object().from(getDownloader().get(firstPageApiUrl, getHeaders(getOriginalUrl())).responseBody());
            if (firstPageApiUrl.contains(GET_SEASON_ARCHIVES_ARCHIVE_BASE_URL)) {
                type = "seasons_archives";
            } else if (firstPageApiUrl.contains(GET_SERIES_BASE_URL)) {
                type = "series";
            } else if (firstPageApiUrl.contains(GET_PARTITION_URL)) {
                type = "partition";
                return;
            }
            data = data.getObject("data");
        } catch (JsonParserException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        if (data != null && data.getObject("meta") != null) {
            final JsonObject metadata = data.getObject("meta");
            final String name = metadata.getString("name", metadata.getString("title", ""));
            if (!name.isEmpty()) {
                return name;
            }
        }
        final String playlistUrl = getLinkHandler().getUrl();
        final String[] nameParts = playlistUrl.split("[?&]name=", 2);
        if (nameParts.length == 2) {
            try {
                return URLDecoder.decode(nameParts[1].split("&", 2)[0], "UTF-8");
            } catch (final UnsupportedEncodingException ignored) {
                return nameParts[1].split("&", 2)[0];
            }
        }
        return "Bilibili Playlist";
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        if (type.equals("partition")) {
            InfoItemsCollector<StreamInfoItem, StreamInfoItemExtractor> collector = new StreamInfoItemsCollector(getServiceId());
            JsonArray relatedArray = data.getArray("data");
            for (int i = 0; i < relatedArray.size(); i++) {
                collector.commit(
                        new BilibiliRelatedInfoItemExtractor(
                                relatedArray.getObject(i), getLinkHandler().getUrl().split("bvid=")[1].split("&")[0],
                                URLDecoder.decode(getLinkHandler().getUrl().split("thumbnail=")[1].split("&")[0], "UTF-8"),
                                String.valueOf(i + 1), getUploaderName(), null));
            }
            if (ServiceList.BiliBili.getFilterTypes().contains("playlists")) {
                collector.applyBlocking(ServiceList.BiliBili.getFilterConfig());
            }
            return new InfoItemsPage<>(collector, null);
        }
        final String firstPageUrl = firstPageApiUrl.isEmpty() ? getUrl() : firstPageApiUrl;
        return getPage(new Page(firstPageUrl + "&username=" + getUploaderName(), getDefaultCookies()));
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(Page page) throws IOException, ExtractionException {
        // Partitions have only 1 page
        type = page.getUrl().contains(GET_SEASON_ARCHIVES_ARCHIVE_BASE_URL) ? "seasons_archives" : "archives";
        try {
            if (!(page.getUrl().contains("pn=1") || page.getUrl().contains("page_num=1"))) {
                data = JsonParser.object().from(getDownloader().get(page.getUrl(), getHeaders(getOriginalUrl())).responseBody()).getObject("data");
            }
        } catch (JsonParserException e) {
            throw new RuntimeException(e);
        }
        JsonArray results = data.getArray("archives");
        if (results.size() == 0) {
            return new InfoItemsPage<>(new StreamInfoItemsCollector(getServiceId()), null);
        }
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        for (int i = 0; i < results.size(); i++) {
            collector.commit(new BilibiliChannelInfoItemWebAPIExtractor(results.getObject(i), getUploaderName(), getUploaderAvatarUrl()));
        }
        if (ServiceList.BiliBili.getFilterTypes().contains("playlists")) {
            collector.applyBlocking(ServiceList.BiliBili.getFilterConfig());
        }
        return new InfoItemsPage<>(collector, new Page(utils.getNextPageFromCurrentUrl(page.getUrl(), type.equals("seasons_archives") ? "page_num" : "pn", 1), getDefaultCookies()));
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        if (type.equals("partition")) {
            try {
                return URLDecoder.decode(getLinkHandler().getUrl().split("uploaderUrl=")[1].split("&")[0], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        final String mid = data != null && data.getObject("meta") != null
                ? data.getObject("meta").getString("mid")
                : utils.getMidFromRecordApiUrl(firstPageApiUrl);
        return BilibiliChannelLinkHandlerFactory.baseUrl + mid;
    }

    @Override
    public String getUploaderName() throws ParsingException {
        if (type.equals("partition")) {
            return getLinkHandler().getUrl().split("uploaderName=")[1].split("&")[0];
        }
        return getUserInfoLazily().getObject("data").getObject("card").getString("name", "[Unknown]");
    }

    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        if (type.equals("partition")) {
            try {
                return URLDecoder.decode(getLinkHandler().getUrl().split("uploaderAvatar=")[1].split("&")[0], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        return getUserInfoLazily().getObject("data").getObject("card").getString("face", "").replace("http:", "https:");
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Override
    public long getStreamCount() throws ParsingException {
        if (type.equals("partition")) {
            return data.getArray("data").size();
        }
        return data.getObject("page").getLong("total");
    }

    @Nonnull
    @Override
    public String getThumbnailUrl() throws ParsingException {
        if (type.equals("partition")) {
            try {
                return URLDecoder.decode(getLinkHandler().getUrl().split("thumbnail=")[1].split("&")[0], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        } else {
            return data.getObject("meta").getString("cover");
        }
    }

    // Get user metadata JSON lazily, without passible NPE.
    private JsonObject getUserInfoLazily() {
        if (userData != null) return userData;
        try {
            userData = JsonParser.object().from(getDownloader().get(getUserInfoRequestUrl(), getHeaders(getOriginalUrl())).responseBody());
        } catch (Exception e) {
            userData = new JsonObject();
        }
        return userData;
    }

    private String getUserInfoRequestUrl() {
        String uid = null;
        // Get from JSON directly
        if (data != null) {
            uid = data.getObject("meta").getString("mid");
        }
        // Use fallback
        if (uid == null) uid = utils.getMidFromRecordApiUrl(firstPageApiUrl);
        return QUERY_USER_INFO_URL + uid;
    }
}
