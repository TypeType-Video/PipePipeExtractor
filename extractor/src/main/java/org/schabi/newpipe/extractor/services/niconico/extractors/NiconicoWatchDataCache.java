package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.*;
import org.schabi.newpipe.extractor.services.niconico.NiconicoService;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;

public class NiconicoWatchDataCache {
    private static final String GUEST_WATCH_API = "https://www.nicovideo.jp/api/watch/v3_guest/";
    private static final String TRACK_ID_LETTERS =
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    String lastId;
    Response response;
    Document page;
    JsonObject watchData;
    WatchDataType watchDataType;
    private String threadServer;
    private String threadId;
    private long startAt;
    private String streamCookie="";

    public enum WatchDataType {
        LOGIN(1),
        GUEST(0);

        private final int value;

        WatchDataType(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
    public void invalidate(){
        lastId = null;
    }

    @Nonnull
    public JsonObject refreshAndGetWatchData(final Downloader downloader,
                                             @Nonnull final String id) throws ExtractionException {
        if (lastId != null && lastId.equals(id)) {
            return watchData;
        }

        if (!id.contains("live.nicovideo.jp") && !ServiceList.NicoNico.hasTokens()) {
            final JsonObject guest = fetchGuestWatchData(downloader, id);
            if (guest != null) {
                response = null;
                page = Jsoup.parse("");
                watchData = guest;
                watchDataType = WatchDataType.GUEST;
                checkPlayable();
                lastId = id;
                return watchData;
            }
        }

        String url = NiconicoService.WATCH_URL + id;
        if(id.contains("live.nicovideo.jp")){
            url = id;
        }
        try {
            HashMap<String, List<String>> headers = new HashMap<>();
            headers.put("User-Agent", Collections.singletonList("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"));
            if(ServiceList.NicoNico.hasTokens()){
                headers.put("Cookie", Collections.singletonList(ServiceList.NicoNico.getTokens()));
            }
            response = downloader.get(url, headers, NiconicoService.LOCALE);
        } catch (final IOException | ReCaptchaException e) {
            throw new ExtractionException("Could not get response.", e);
        }
        page = Jsoup.parse(response.responseBody());
        try {
            Element element = page.select("meta[name=\"server-response\"]").first();
            if (element == null) {
                watchDataType = WatchDataType.LOGIN; //need login
                if(response.responseBody().contains("チャンネル会員専用動画")){
                    throw new PaidContentException("Channel member limited videos");
                } else if (response.responseBody().contains("地域と同じ地域からのみ視聴")) {
                    throw new GeographicRestrictionException("Sorry, this video can only be viewed in the same region where it was uploaded.");
                } else if (response.responseBody().contains("この動画を視聴するにはログインが必要です。")) {
                    throw new PaidContentException("This video requires login to view.");
                }
                throw new ContentNotAvailableException(page.select("p.fail-message").text());
            } else {
                watchDataType = WatchDataType.GUEST;
            }
            if (watchDataType == WatchDataType.LOGIN) {
                watchData = JsonParser.object().from(
                        page.getElementsByClass("content WatchAppContainer")
                                .attr("data-video"));
            } else {
                final JsonObject responseData = JsonParser.object().from(
                        element.attr("content")).getObject("data").getObject("response");
                // the watch data is nested in a $watchV4 object since NicoNico's October 2026 change
                watchData = responseData.has("$watchV4")
                        ? responseData.getObject("$watchV4").getObject("data")
                        : responseData;
            }
        } catch (JsonParserException e) {
            throw new ParsingException("Failed to parse content");
        }

        checkPlayable();
        lastId = id;
        return watchData;
    }

    // The guest watch API answers in about 0.35 s from Europe where the watch page takes about
    // 0.6 s, with the watch data in the shape the page used before October 2026. Any surprise
    // falls back to the page, which also explains why a video cannot be watched.
    private static JsonObject fetchGuestWatchData(final Downloader downloader, final String id) {
        try {
            final HashMap<String, List<String>> headers = new HashMap<>();
            headers.put("X-Frontend-Id", Collections.singletonList("6"));
            headers.put("X-Frontend-Version", Collections.singletonList("0"));
            final Response guest = downloader.get(GUEST_WATCH_API + id + "?actionTrackId="
                    + actionTrackId(), headers, NiconicoService.LOCALE);
            if (guest.responseCode() != 200) {
                return null;
            }
            final JsonObject root = JsonParser.object().from(guest.responseBody());
            if (root.getObject("meta").getInt("status") != 200) {
                return null;
            }
            final JsonObject data = root.getObject("data");
            final JsonObject domand = data.getObject("media").getObject("domand");
            if (domand.getArray("videos").isEmpty() || domand.getString("accessRightKey") == null
                    || data.getObject("client").getString("watchTrackId") == null) {
                return null;
            }
            return data;
        } catch (final Exception e) {
            return null;
        }
    }

    private static String actionTrackId() {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            builder.append(TRACK_ID_LETTERS.charAt(RANDOM.nextInt(TRACK_ID_LETTERS.length())));
        }
        return builder.append('_').append(System.currentTimeMillis()).toString();
    }

    private void checkPlayable() throws ExtractionException {
        if (watchData.getString("errorCode") != null && watchData.getString("errorCode").equals("FORBIDDEN")) { //TODO: also for other types such as member limited vids
            switch (watchData.getString("reasonCode")) {
                case "DOMESTIC_VIDEO":
                    throw new GeographicRestrictionException("This video is only available in Japan");
                case "HARMFUL_VIDEO":
                    throw new NeedLoginException("This content need an account to view");
                default:
                    throw new ContentNotAvailableException(watchData.getString("reasonCode"));
            }
        } else if ("PAYMENT_PREVIEW_SUPPORTED".equals(watchData.getString("okReason"))) {
            final JsonObject payment = watchData.getObject("payment");
            final JsonObject videoPayment = payment.getObject("video");
            final String billingType = videoPayment.getString("billingType",
                    payment.getString("billingType", ""));
            if (videoPayment.getBoolean("isPremium")
                    || "premium_only".equals(payment.getString("watchableUserType", ""))) {
                throw new PaidContentException("This content is limited to premium users");
            } else if ("member_only".equals(billingType)) {
                throw new PaidContentException("This content is limited to channel members");
            }
        }
    }

    public JsonObject getLastWatchData() {
        return watchData;
    }

    public WatchDataType getLastWatchDataType() {
        return watchDataType;
    }

    public Document getLastPage() {
        return page;
    }

    public Response getLastResponse() {
        return response;
    }

    public String getThreadServer() {
        return threadServer;
    }

    public void setThreadServer(String threadServer) {
        this.threadServer = threadServer;
    }

    public long getStartAt() {
        return startAt;
    }

    public void setStartAt(long startAt) {
        this.startAt = startAt;
    }

    public String getStreamCookie() {
        if(ServiceList.NicoNico.hasTokens()){
            String cookie = ServiceList.NicoNico.getTokens();
            if(cookie.endsWith(";")){
                cookie = cookie.substring(0, cookie.length() - 1);
            }
            cookie += ";" + streamCookie;
            return cookie;
        }
        return streamCookie;
    }

    public void setStreamCookie(String streamCookie) {
        this.streamCookie = streamCookie;
    }
}
