package org.schabi.newpipe.extractor.services.youtube.extractors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;

import org.junit.jupiter.api.Test;

class YoutubeCoursePlaylistItemTest {
    private static final String COVER = "\"thumbnail\":{\"thumbnails\":["
            + "{\"url\":\"https://i.ytimg.com/vi/a/default.jpg\",\"width\":120,\"height\":90},"
            + "{\"url\":\"//i.ytimg.com/vi/a/hqdefault.jpg\",\"width\":480,\"height\":270}]}";

    private static YoutubeMixOrPlaylistInfoItemExtractor item(final String thumbnailRenderer)
            throws Exception {
        final JsonObject renderer = JsonParser.object().from("{\"playlistId\":\"PLcourse\","
                + "\"title\":{\"simpleText\":\"Course\"},\"videoCount\":\"12\","
                + "\"thumbnailRenderer\":" + thumbnailRenderer + "}");
        return new YoutubeMixOrPlaylistInfoItemExtractor(renderer);
    }

    @Test
    void readsTheCoverOfACourseFromItsFirstVideo() throws Exception {
        assertEquals("https://i.ytimg.com/vi/a/hqdefault.jpg",
                item("{\"playlistVideoThumbnailRenderer\":{" + COVER + "}}").getThumbnailUrl());
    }

    @Test
    void keepsReadingACustomCover() throws Exception {
        assertEquals("https://i.ytimg.com/vi/a/hqdefault.jpg",
                item("{\"playlistCustomThumbnailRenderer\":{" + COVER + "}}").getThumbnailUrl());
    }

    @Test
    void hasNoThumbnailWhenTheItemCarriesNone() throws Exception {
        assertNull(item("{}").getThumbnailUrl());
    }
}
