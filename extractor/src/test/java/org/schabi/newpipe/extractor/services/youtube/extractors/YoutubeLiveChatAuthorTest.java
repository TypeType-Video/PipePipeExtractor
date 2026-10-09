package org.schabi.newpipe.extractor.services.youtube.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItemsCollector;

import static org.junit.jupiter.api.Assertions.*;

class YoutubeLiveChatAuthorTest {
    private static final String MESSAGE = """
            {
              "authorName": {"simpleText": "Camille"},
              "authorPhoto": {"thumbnails": [
                {"url": "https://yt3.ggpht.com/small", "width": 32},
                {"url": "https://yt3.ggpht.com/large", "width": 64}
              ]},
              "authorBadges": [{"liveChatAuthorBadgeRenderer": {
                "icon": {"iconType": "MODERATOR"}
              }}],
              "message": {"runs": [{"text": "hello"}]},
              "purchaseAmountText": {"simpleText": "$5"}
            }
            """;

    @Test
    void carriesRealAuthorsThroughTheCollector() throws Exception {
        final JsonObject data = JsonParser.object().from(MESSAGE);
        final var collector = new BulletCommentsInfoItemsCollector(0);
        final var item = collector.extract(new YoutubeBulletCommentsInfoItemExtractor(data, 0, -1));
        assertEquals("hello", item.getCommentText());
        assertEquals("Camille", item.getAuthorName());
        assertEquals("https://yt3.ggpht.com/large", item.getAuthorAvatarUrl());
        assertTrue(item.isModerator());
    }

    @Test
    void paidMessagesRetainTheirAuthors() throws Exception {
        final JsonObject data = JsonParser.object().from(MESSAGE);
        final var item = new BulletCommentsInfoItemsCollector(0)
                .extract(new YoutubeSuperChatInfoItemExtractor(data, 0, -1));
        assertEquals("Camille", item.getAuthorName());
        assertEquals("https://yt3.ggpht.com/large", item.getAuthorAvatarUrl());
        assertTrue(item.isModerator());
        assertEquals("($5) hello", item.getCommentText());
    }

    @Test
    void absentMetadataDoesNotInventAnAuthor() throws Exception {
        final JsonObject data = JsonParser.object().from("{\"message\":{\"runs\":[{\"text\":\"hello\"}]}}");
        final var item = new BulletCommentsInfoItemsCollector(0)
                .extract(new YoutubeBulletCommentsInfoItemExtractor(data, 0, -1));
        assertEquals("hello", item.getCommentText());
        assertNull(item.getAuthorName());
        assertNull(item.getAuthorAvatarUrl());
        assertFalse(item.isModerator());
    }

    @Test
    void memberBadgesAreNotModeratorBadges() throws Exception {
        final JsonObject data = JsonParser.object().from("""
                {"authorName":{"runs":[{"text":"Mina"}]},
                 "authorBadges":[{"liveChatAuthorBadgeRenderer":{
                   "icon":{"iconType":"SPONSOR"}}}]}
                """);
        final var extractor = new YoutubeBulletCommentsInfoItemExtractor(data, 0, -1);
        assertEquals("Mina", extractor.getAuthorName());
        assertFalse(extractor.isModerator());
    }
}
