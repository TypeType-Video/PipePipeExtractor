package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsExtractor;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItem;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItemsCollector;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.services.bilibili.BilibiliWebSocketClient;
import org.schabi.newpipe.extractor.services.bilibili.WatchDataCache;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.*;

public class BilibiliBulletCommentsExtractor extends BulletCommentsExtractor {

    private long cid;
    private long roomId;
    private long startTime;
    private Document result;
    private BilibiliWebSocketClient webSocketClient;
    private boolean isLive = false;
    private final WatchDataCache watchDataCache;

    public BilibiliBulletCommentsExtractor(StreamingService service, ListLinkHandler uiHandler, WatchDataCache watchDataCache) {
        super(service, uiHandler);
        this.watchDataCache = watchDataCache;
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {
        if (getUrl().contains(LIVE_BASE_URL)) {
            try {
                final Long cachedRoomId = watchDataCache.findRoomId(getId());
                roomId = cachedRoomId != null && cachedRoomId > 0
                        ? cachedRoomId
                        : watchDataCache.getRoomId();
                final Long cachedStartTime = watchDataCache.findStartTime(getId());
                startTime = cachedStartTime != null ? cachedStartTime : watchDataCache.getStartTime();
                final Response danmuInfoResponse = downloader.get(
                        QUERY_DANMU_INFO_URL + roomId, getHeaders(LIVE_REFERER));
                BilibiliRiskControl.check(danmuInfoResponse);
                final JsonObject danmuInfo = JsonParser.object().from(danmuInfoResponse.responseBody());
                final JsonObject data = danmuInfo.getObject("data");
                final String token = data == null ? null : data.getString("token");
                if (token == null || token.isEmpty()) {
                    throw new ParsingException("Failed to connect to live chat: missing token");
                }
                webSocketClient = new BilibiliWebSocketClient(roomId, token);
                webSocketClient.getWebSocketClient().connectBlocking();
                isLive = true;
            } catch (URISyntaxException | InterruptedException e) {
                throw new RuntimeException(e);
            } catch (JsonParserException e) {
                throw new ParsingException("Failed to connect to live chat", e);
            }
            return;
        }
        final Long cachedCid = watchDataCache.findCid(getId());
        if (cachedCid != null && cachedCid > 0) {
            cid = cachedCid;
        } else {
            cid = resolveCid(downloader);
        }
        final Response dmResponse = downloader.get(
                QUERY_VIDEO_BULLET_COMMENTS_URL + cid, getHeaders(getOriginalUrl()));
        result = BilibiliBulletCommentsXmlDecoder.decode(dmResponse);
    }

    private long resolveCid(@Nonnull final Downloader downloader) throws IOException, ExtractionException {
        final String id = getId();
        if (getUrl().contains("bangumi/play/")) {
            final BilibiliStreamMetadataHelper.BangumiMetadata metadata =
                    BilibiliStreamMetadataHelper.fetchBangumiMetadata(downloader, id, getOriginalUrl());
            BilibiliStreamMetadataHelper.cacheIds(watchDataCache, id, metadata.bvid, metadata.cid);
            return metadata.cid;
        }
        final BilibiliStreamMetadataHelper.VideoMetadata metadata =
                BilibiliStreamMetadataHelper.fetchVideoMetadata(
                        downloader, id, getLinkHandler().getUrl(), getOriginalUrl());
        BilibiliStreamMetadataHelper.cacheIds(watchDataCache, id, metadata.bvid, metadata.cid);
        return metadata.cid;
    }

    @Override
    public List<BulletCommentsInfoItem> getLiveMessages() {
        if (webSocketClient == null) {
            return Collections.emptyList();
        }
        final BulletCommentsInfoItemsCollector collector =
                new BulletCommentsInfoItemsCollector(getServiceId());
        ArrayList<JsonObject> messages = webSocketClient.getMessages();
        for (final JsonObject message : messages) {
            String cmd = message.getString("cmd");
            try {
                if (cmd.equals("DANMU_MSG")) {
                    collector.commit(new BilibiliLiveBulletCommentsInfoItemExtractor(message, startTime));
                } else if (cmd.contains("SUPER_CHAT_MESSAGE")) {
                    collector.commit(new BilibiliSuperChatInfoItemExtractor(message, startTime));
                }

            } catch (JsonParserException e) {
                throw new RuntimeException(e);
            }
        }
        return new InfoItemsPage<>(collector, null).getItems();
    }

    @Nonnull
    @Override
    public InfoItemsPage<BulletCommentsInfoItem> getInitialPage() throws IOException, ExtractionException {
        final BulletCommentsInfoItemsCollector collector =
                new BulletCommentsInfoItemsCollector(getServiceId());
        if (getUrl().contains(LIVE_BASE_URL)) {
            return new InfoItemsPage<>(collector, null);
        }
        if (result == null || "1".equals(result.select("state").text().trim())) {
            return new InfoItemsPage<>(collector, null);
        }
        final Elements elements = result.select("d");
        for (final Element element : elements) {
            final String p = element.attr("p");
            final String[] attrs = p.split(",");
            if (attrs.length < 6) {
                continue;
            }
            final int pool;
            try {
                pool = Integer.parseInt(attrs[5].trim());
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (pool == 3) { // voting
                continue;
            }
            final String mode = attrs[1].trim();
            if ("8".equals(mode) || "9".equals(mode)) { // code / BAS script danmaku
                continue;
            }
            try {
                final double timeSec = Double.parseDouble(attrs[0].trim());
                if (!Double.isFinite(timeSec) || timeSec < 0) {
                    continue;
                }
            } catch (NumberFormatException ignored) {
                continue;
            }
            collector.commit(new BilibiliBulletCommentsInfoItemExtractor(element, attrs));
        }
        return new InfoItemsPage<>(collector, null);
    }

    @Override
    public boolean isLive() {
        return isLive;
    }

    @Override
    public void disconnect() {
        if (webSocketClient != null) {
            webSocketClient.disconnect();
        }
    }

    @Override
    public void reconnect() {
        if (webSocketClient != null && webSocketClient.getWebSocketClient().isClosed()) {
            try {
                webSocketClient.wrappedReconnect();
            } catch (URISyntaxException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
