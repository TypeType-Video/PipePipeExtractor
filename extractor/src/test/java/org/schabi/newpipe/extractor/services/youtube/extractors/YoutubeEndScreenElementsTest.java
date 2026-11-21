package org.schabi.newpipe.extractor.services.youtube.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.stream.EndScreenElement;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeEndScreenElementsTest {
    private static List<EndScreenElement> parse(final String elements) throws Exception {
        final JsonObject endScreen = JsonParser.object().from(
                "{\"endscreenRenderer\":{\"elements\":[" + elements + "]}}");
        return YoutubeStreamExtractor.parseEndScreenElements(endScreen);
    }

    private static String element(final String fields) {
        return "{\"endscreenElementRenderer\":{" + fields + "}}";
    }

    @Test
    void readsVideoElementWithTimingGeometryAndThumbnail() throws Exception {
        final EndScreenElement video = parse(element("\"id\":\"a\",\"style\":\"VIDEO\","
                + "\"title\":{\"simpleText\":\"Next one\"},"
                + "\"image\":{\"thumbnails\":[{\"url\":\"https://i.ytimg.com/vi/x/mq.jpg\","
                + "\"width\":320,\"height\":180}]},"
                + "\"startMs\":\"590000\",\"endMs\":\"610000\","
                + "\"left\":0.02,\"top\":0.45,\"width\":0.33,\"aspectRatio\":1.77,"
                + "\"endpoint\":{\"commandMetadata\":{\"webCommandMetadata\":"
                + "{\"url\":\"/watch?v=x&pp=0gcJCQoB\"}}}")).get(0);

        assertEquals("VIDEO", video.type());
        assertEquals("Next one", video.title());
        assertEquals("https://www.youtube.com/watch?v=x", video.url());
        assertEquals(590000L, video.startTimeMs());
        assertEquals(610000L, video.endTimeMs());
        assertEquals(0.02, video.left());
        assertEquals(0.33, video.width());
        assertEquals("https://i.ytimg.com/vi/x/mq.jpg", video.thumbnails().get(0).getUrl());
    }

    @Test
    void exposesTheDestinationOfAnExternalLinkInsteadOfTheRedirect() throws Exception {
        final EndScreenElement link = parse(element("\"id\":\"b\",\"style\":\"WEBSITE\","
                + "\"endpoint\":{\"urlEndpoint\":{\"url\":"
                + "\"https://www.youtube.com/redirect?q=https%3A%2F%2Fexample.org%2Fpage\"}}"))
                .get(0);

        assertEquals("https://example.org/page", link.url());
        assertEquals(-1L, link.startTimeMs());
        assertEquals(-1.0, link.left());
        assertTrue(link.thumbnails().isEmpty());
    }

    @Test
    void keepsUnknownTypesAndSkipsOnlyTheMalformedElement() throws Exception {
        final List<EndScreenElement> elements = parse(
                element("\"id\":\"bad\",\"style\":\"VIDEO\",\"startMs\":\"soon\"") + ","
                        + "{\"somethingElse\":{}},"
                        + element("\"id\":\"new\",\"style\":\"FUTURE_KIND\""));

        assertEquals(1, elements.size());
        assertEquals("new", elements.get(0).id());
        assertEquals("FUTURE_KIND", elements.get(0).type());
    }

    @Test
    void returnsNothingWithoutAnEndScreen() {
        assertTrue(YoutubeStreamExtractor.parseEndScreenElements(new JsonObject()).isEmpty());
    }
}
