package org.schabi.newpipe.extractor.services.youtube.sabr;

import com.grack.nanojson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeSabrAudioOnlyMediaTest {
    private static final String AUDIO =
            "{\"itag\":140,\"lastModified\":\"1\",\"mimeType\":\"audio/mp4\","
                    + "\"bitrate\":128000,\"contentLength\":\"90000\",\"approxDurationMs\":\"5456\"}";
    private static final String VIDEO =
            "{\"itag\":299,\"lastModified\":\"2\",\"mimeType\":\"video/mp4\",\"width\":1920,"
                    + "\"height\":1080,\"bitrate\":4000000,\"contentLength\":\"2000\","
                    + "\"approxDurationMs\":\"5456\"}";

    @Test
    void mediaWithoutVideoStaysAudioOnly() throws Exception {
        final YoutubeSabrInfo info = info("[" + AUDIO + "]");
        final YoutubeSabrFormat audio = info.getFormats().get(0);
        final YoutubeSabrSession session = new YoutubeSabrSession(info, audio, audio);
        final YoutubeSabrStreamState state = session.getStreamState();

        state.setActiveTrackTypes(true, true);
        state.setVideoAndAudioRequestMode();

        assertTrue(state.isAudioOnlyMedia());
        assertEquals(YoutubeSabrStreamState.TRACK_MODE_AUDIO_ONLY, state.getEnabledTrackTypesBitfield());
        assertTrue(state.shouldPreferAudioFormat());
        assertFalse(state.shouldPreferVideoFormat());
        assertFalse(state.shouldSelectVideoFormat());
        assertTrue(state.getBufferedRanges().isEmpty());
    }

    @Test
    void mediaWithVideoStillNeedsTwoDistinctFormats() throws Exception {
        final YoutubeSabrInfo info = info("[" + AUDIO + "," + VIDEO + "]");
        final YoutubeSabrFormat audio = info.getFormats().get(0);

        assertThrows(IllegalArgumentException.class, () -> new YoutubeSabrSession(info, audio, audio));
        assertFalse(new YoutubeSabrSession(info, audio, info.getFormats().get(1))
                .getStreamState().isAudioOnlyMedia());
    }

    private static YoutubeSabrInfo info(final String formatsJson) throws Exception {
        final List<YoutubeSabrFormat> formats = YoutubeSabrFormat.fromAdaptiveFormats(
                "voice", JsonParser.array().from(formatsJson));
        return new YoutubeSabrInfo(
                YoutubeSabrClientProfile.MWEB,
                "voice",
                "cpn",
                YoutubeSabrClientProfile.MWEB.getClientVersion(),
                "visitor",
                "https://example.com/sabr",
                "AA==",
                formats);
    }
}
