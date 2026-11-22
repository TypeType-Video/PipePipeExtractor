package org.schabi.newpipe.extractor.services.youtube.sabr;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonParser;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.downloader.StreamingResponse;
import org.schabi.newpipe.extractor.localization.Localization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeSabrSessionReloadTest {
    private static final Localization LOCALIZATION = new Localization("en", "US");
    // reloadPlaybackParams.token carried by a live part 46, as reported in the SABR reload issue.
    private static final String TOKEN =
            "Cj8aAwoBASILa0lvMkJBdWJPNmsqJDNpMnUvSzlvemdseURUUy9qZ1NtSklnVUFEcjNveDFWcTU0djCQteCV"
                    + "njQSJQEyjDJ5oJTjWD6MZq1_Q_dvW6YDrTBPJh4ed9vXps3v80eL_R4%3D";

    @Test
    void refusedReloadEndsWithTheYoutubeReasonInsteadOfASpentBudget() throws Exception {
        final List<String> tokens = new ArrayList<>();
        final Fixture fixture = fixture((videoId, profile, localization, country, provider,
                                         reloadToken) -> {
            tokens.add(reloadToken);
            throw new SabrProtocolException("Player response has no streamingData for MWEB"
                    + " (UNPLAYABLE: This content isn't available.)");
        });

        final SabrReloadException error = assertThrows(SabrReloadException.class,
                () -> fixture.pump());

        assertEquals(List.of(TOKEN), tokens);
        assertEquals(TOKEN, error.getToken());
        assertTrue(error.getMessage().startsWith("SABR player reload 1/2 refused: "));
        assertTrue(error.getMessage().contains("UNPLAYABLE: This content isn't available."));
    }

    @Test
    void reloadWithoutStreamingUrlIsARefusal() throws Exception {
        final YoutubeSabrInfo noUrl = info("visitor", "");
        final Fixture fixture = fixture((videoId, profile, localization, country, provider,
                                         reloadToken) -> noUrl);

        final SabrReloadException error = assertThrows(SabrReloadException.class,
                () -> fixture.pump());

        assertEquals("SABR player reload 1/2 refused: no streaming URL", error.getMessage());
    }

    @Test
    void acceptedReloadsMoveToTheNewUrlUntilTheBudgetIsSpent() throws Exception {
        final YoutubeSabrInfo fresh = info("visitor", "https://example.com/fresh");
        final Fixture fixture = fixture((videoId, profile, localization, country, provider,
                                         reloadToken) -> fresh);

        final SabrReloadException error = assertThrows(SabrReloadException.class, () -> {
            for (int i = 0; i < 5; i++) {
                fixture.pump();
            }
        });

        assertTrue(error.getMessage().contains("(reload budget spent)"));
        assertEquals(3, fixture.downloader.urls.size());
        assertTrue(fixture.downloader.urls.get(1).startsWith("https://example.com/fresh"));
        assertTrue(fixture.downloader.urls.get(2).startsWith("https://example.com/fresh"));
    }

    @Test
    void playabilityIsNamedWhenStreamingDataIsMissing() throws Exception {
        assertEquals(" (UNPLAYABLE: This content isn't available.)",
                YoutubeSabrProbe.describePlayability(JsonParser.object().from(
                        "{\"playabilityStatus\":{\"status\":\"UNPLAYABLE\","
                                + "\"reason\":\"This content isn't available.\"}}")));
        assertEquals("", YoutubeSabrProbe.describePlayability(JsonParser.object().from("{}")));
    }

    @Nonnull
    private static Fixture fixture(@Nonnull final YoutubeSabrSession.PlayerInfoReloader reloader)
            throws Exception {
        final FakeDownloader downloader = new FakeDownloader(reloadResponse());
        NewPipe.init(downloader);
        final YoutubeSabrInfo info = info("visitor", "https://example.com/sabr");
        final List<YoutubeSabrFormat> formats = info.getFormats();
        final YoutubeSabrSession session = new YoutubeSabrSession(
                info, formats.get(0), formats.get(1), null, null, reloader);
        return new Fixture(session, downloader, formats.get(1));
    }

    @Nonnull
    private static YoutubeSabrInfo info(@Nonnull final String visitorData,
                                        @Nonnull final String streamingUrl) throws Exception {
        final JsonArray formats = JsonParser.array().from("["
                + "{\"itag\":140,\"lastModified\":\"1\",\"mimeType\":\"audio/mp4\","
                + "\"bitrate\":128000,\"contentLength\":\"1000\","
                + "\"approxDurationMs\":\"60000\"},"
                + "{\"itag\":299,\"lastModified\":\"2\",\"mimeType\":\"video/mp4\","
                + "\"width\":1920,\"height\":1080,\"bitrate\":4000000,"
                + "\"contentLength\":\"2000\",\"approxDurationMs\":\"60000\"}]");
        return new YoutubeSabrInfo(YoutubeSabrClientProfile.MWEB, "video", "cpn",
                YoutubeSabrClientProfile.MWEB.getClientVersion(), visitorData, streamingUrl,
                "AA==", YoutubeSabrFormat.fromAdaptiveFormats("video", formats), false);
    }

    @Nonnull
    private static byte[] reloadResponse() {
        final SabrProto.Writer params = new SabrProto.Writer();
        params.writeBytes(1, TOKEN.getBytes(StandardCharsets.UTF_8));
        final SabrProto.Writer part = new SabrProto.Writer();
        part.writeMessage(1, params.toByteArray());
        final byte[] payload = part.toByteArray();
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(SabrResponseDecoder.RELOAD_PLAYER_RESPONSE);
        // UMP two-byte length: six low bits in the first byte, the rest in the second.
        output.write(0x80 | (payload.length & 0x3f));
        output.write(payload.length >> 6);
        output.write(payload, 0, payload.length);
        return output.toByteArray();
    }

    private static final class Fixture {
        @Nonnull private final YoutubeSabrSession session;
        @Nonnull private final FakeDownloader downloader;
        @Nonnull private final YoutubeSabrFormat videoFormat;

        private Fixture(@Nonnull final YoutubeSabrSession session,
                        @Nonnull final FakeDownloader downloader,
                        @Nonnull final YoutubeSabrFormat videoFormat) {
            this.session = session;
            this.downloader = downloader;
            this.videoFormat = videoFormat;
        }

        void pump() throws Exception {
            session.pumpOnceStreamingForDemand(LOCALIZATION,
                    SabrSegmentRequest.media(videoFormat, 1));
        }
    }

    private static final class FakeDownloader extends Downloader {
        @Nonnull private final byte[] body;
        @Nonnull private final List<String> urls = new ArrayList<>();

        private FakeDownloader(@Nonnull final byte[] body) {
            this.body = body.clone();
        }

        @Override
        public StreamingResponse postStreaming(
                final String url,
                @Nullable final Map<String, List<String>> headers,
                @Nullable final byte[] dataToSend,
                @Nullable final Localization localization) {
            urls.add(url);
            return new StreamingResponse(200,
                    Collections.singletonMap("Content-Type",
                            Collections.singletonList("application/vnd.yt-ump")),
                    new ByteArrayInputStream(body));
        }

        @Override
        public Response execute(@Nonnull final Request request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CancellableCall executeAsync(@Nonnull final Request request,
                                            final AsyncCallback callback) {
            throw new UnsupportedOperationException();
        }
    }
}
