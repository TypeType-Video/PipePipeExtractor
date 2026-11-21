package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.services.bilibili.WatchDataCache;
import org.schabi.newpipe.extractor.services.bilibili.utils;

import javax.annotation.Nonnull;
import java.io.IOException;

import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.getHeaders;

final class BilibiliStreamMetadataHelper {
    static final int RISK_CONTROL_CODE = -352;
    static final int RISK_CONTROL_PRECONDITION = -412;

    static final class BangumiMetadata {
        final JsonObject seasonResult;
        final JsonArray episodes;
        final JsonObject episode;
        final String bvid;
        final long cid;

        BangumiMetadata(final JsonObject seasonResult,
                        final JsonArray episodes,
                        final JsonObject episode,
                        final String bvid,
                        final long cid) {
            this.seasonResult = seasonResult;
            this.episodes = episodes;
            this.episode = episode;
            this.bvid = bvid;
            this.cid = cid;
        }
    }

    static final class VideoMetadata {
        final JsonObject watch;
        final JsonObject page;
        final String bvid;
        final long cid;

        VideoMetadata(final JsonObject watch,
                      final JsonObject page,
                      final String bvid,
                      final long cid) {
            this.watch = watch;
            this.page = page;
            this.bvid = bvid;
            this.cid = cid;
        }
    }

    private BilibiliStreamMetadataHelper() {
    }

    static BangumiMetadata fetchBangumiMetadata(@Nonnull final Downloader downloader,
                                                final String id,
                                                final String originalUrl)
            throws IOException, ExtractionException {
        if (id == null || id.length() <= 2) {
            throw new ParsingException("Invalid Bilibili bangumi id: " + id);
        }
        final boolean isSeason = id.startsWith("ss");
        final String epOrSeasonId = id.substring(2);
        final String seasonUrl = "https://api.bilibili.com/pgc/view/web/season?"
                + (isSeason ? "season_id=" : "ep_id=") + epOrSeasonId;
        final Response seasonResponse = downloader.get(seasonUrl, getHeaders(originalUrl));
        BilibiliRiskControl.check(seasonResponse);
        if (seasonResponse.responseCode() < 200 || seasonResponse.responseCode() >= 300) {
            throw new ParsingException("Failed to fetch Bilibili bangumi metadata: HTTP "
                    + seasonResponse.responseCode());
        }
        final JsonObject responseJson;
        try {
            responseJson = JsonParser.object().from(seasonResponse.responseBody());
        } catch (JsonParserException e) {
            throw new ParsingException("Could not parse Bilibili bangumi metadata", e);
        }
        checkMetadataCode(responseJson, "Could not get Bilibili bangumi metadata");
        final JsonObject seasonResult = responseJson.getObject("result");
        final JsonArray episodes = seasonResult == null ? null : seasonResult.getArray("episodes");
        if (episodes == null || episodes.isEmpty()) {
            throw new ContentNotAvailableException("Could not get Bilibili bangumi episodes");
        }
        JsonObject episode = null;
        if (isSeason) {
            episode = episodes.getObject(0);
        } else {
            for (int i = 0; i < episodes.size(); i++) {
                final JsonObject candidate = episodes.getObject(i);
                if (candidate.getString("share_url", "").endsWith(id)
                        || String.valueOf(candidate.getLong("id")).equals(epOrSeasonId)) {
                    episode = candidate;
                    break;
                }
            }
        }
        if (episode == null) {
            throw new ContentNotAvailableException("Not found id in series data");
        }
        final long cid = episode.getLong("cid");
        if (cid <= 0) {
            throw new ParsingException("Could not resolve cid for Bilibili bangumi: " + id);
        }
        return new BangumiMetadata(seasonResult, episodes, episode, episode.getString("bvid"), cid);
    }

    static VideoMetadata fetchVideoMetadata(@Nonnull final Downloader downloader,
                                            final String id,
                                            final String streamUrl,
                                            final String originalUrl)
            throws IOException, ExtractionException {
        final String bvid = utils.getPureBV(id).split("#")[0];
        final String metadataUrl = "https://api.bilibili.com/x/web-interface/wbi/view?bvid=" + bvid;
        final Response metadataResponse = downloader.get(metadataUrl, getHeaders(originalUrl));
        BilibiliRiskControl.check(metadataResponse);
        if (metadataResponse.responseCode() < 200 || metadataResponse.responseCode() >= 300) {
            throw new ParsingException("Failed to fetch Bilibili video metadata: HTTP "
                    + metadataResponse.responseCode());
        }
        final JsonObject responseJson;
        try {
            responseJson = JsonParser.object().from(metadataResponse.responseBody());
        } catch (JsonParserException e) {
            throw new ContentNotAvailableException("Could not get Bilibili video metadata", e);
        }
        checkMetadataCode(responseJson, "Could not get Bilibili video metadata");
        final JsonObject watch = responseJson.getObject("data");
        if (watch == null || watch.isEmpty()) {
            throw new ContentNotAvailableException("Could not get Bilibili video metadata");
        }
        final JsonObject page = BilibiliStreamPageParser.pageForUrl(watch, streamUrl);
        long cid = page.getLong("cid");
        if (cid <= 0) {
            cid = watch.getLong("cid");
        }
        if (cid <= 0) {
            throw new ParsingException("Could not resolve cid for Bilibili video: " + bvid);
        }
        return new VideoMetadata(watch, page, bvid, cid);
    }

    static void checkMetadataCode(@Nonnull final JsonObject responseJson,
                                  final String fallbackMessage)
            throws BilibiliRiskControlException, ContentNotAvailableException {
        final int code = responseJson.getInt("code", -1);
        if (code == RISK_CONTROL_CODE || code == RISK_CONTROL_PRECONDITION) {
            throw new BilibiliRiskControlException(
                    "Bilibili risk control blocked metadata lookup", code);
        }
        if (code != 0) {
            throw new ContentNotAvailableException(fallbackMessage + " (code=" + code + ")");
        }
    }

    static void cacheIds(@Nonnull final WatchDataCache cache,
                         final String id,
                         final String bvid,
                         final long cid) {
        cache.setCid(id, cid);
        if (bvid != null && !bvid.isEmpty()) {
            cache.setBvid(id, bvid);
        }
    }
}
