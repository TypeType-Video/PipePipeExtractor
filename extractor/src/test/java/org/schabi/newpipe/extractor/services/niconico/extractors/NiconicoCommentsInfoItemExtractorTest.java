package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonParser;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NiconicoCommentsInfoItemExtractorTest {
    private static final String URL = "https://www.nicovideo.jp/watch/sm9";

    @Test
    void readsTheCurrentCommentFormat() throws Exception {
        final NiconicoCommentsInfoItemExtractor comment = new NiconicoCommentsInfoItemExtractor(
                JsonParser.object().from("{\"id\":\"882123066398695732\",\"no\":4118020,"
                        + "\"vposMs\":361140,\"body\":\"text\",\"userId\":\"user\","
                        + "\"score\":-2200,\"postedAt\":\"2013-02-17T21:16:30+09:00\","
                        + "\"nicoruCount\":157}"),
                URL);

        assertEquals("06:01 text", comment.getCommentText().getContent());
        assertEquals("882123066398695732", comment.getCommentId());
        assertEquals(157, comment.getLikeCount());
        assertEquals(OffsetDateTime.parse("2013-02-17T12:16:30Z").toInstant(),
                comment.getUploadDate().offsetDateTime().toInstant());
    }

    @Test
    void keepsReadingTheLegacyCommentFormat() throws Exception {
        final NiconicoCommentsInfoItemExtractor comment = new NiconicoCommentsInfoItemExtractor(
                JsonParser.object().from("{\"no\":\"12\",\"vpos\":6100,\"content\":\"text\","
                        + "\"score\":3,\"date\":1361103390}"),
                URL);

        assertEquals("01:01 text", comment.getCommentText().getContent());
        assertEquals("12", comment.getCommentId());
        assertEquals(3, comment.getLikeCount());
        assertEquals(1361103390L, comment.getUploadDate().offsetDateTime().toEpochSecond());
    }
}
