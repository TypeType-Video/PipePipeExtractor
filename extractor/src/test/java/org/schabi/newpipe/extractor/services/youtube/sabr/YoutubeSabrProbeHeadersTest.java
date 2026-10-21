package org.schabi.newpipe.extractor.services.youtube.sabr;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YoutubeSabrProbeHeadersTest {
    private static final String TV_USER_AGENT = "Mozilla/5.0 (SMART-TV; X11; Linux armv7l) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.85 Safari/537.36";

    @Test
    void tvSabrRequestsUseTheTvPlayerUserAgent() {
        final YoutubeSabrInfo info = new YoutubeSabrInfo(YoutubeSabrClientProfile.TVHTML5,
                "video", "cpn", "7.test", "visitor", "https://example.googlevideo.com/sabr",
                null, Collections.emptyList());

        final Map<String, List<String>> headers = YoutubeSabrProbe.buildSabrHeaders(info);

        assertEquals(Collections.singletonList(TV_USER_AGENT), headers.get("User-Agent"));
    }
}
