package org.schabi.newpipe.extractor.services.bilibili;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BilibiliVerificationTest {
    @Test
    void searchResultsUseOfficialVerify() throws Exception {
        assertTrue(BilibiliVerification.isVerified(json("{\"official_verify\":{\"type\":0,\"desc\":\"creator\"}}")));
        assertTrue(BilibiliVerification.isVerified(json("{\"official_verify\":{\"type\":1,\"desc\":\"media\"}}")));
        assertFalse(BilibiliVerification.isVerified(json("{\"official_verify\":{\"type\":-1,\"desc\":\"\"}}")));
        assertFalse(BilibiliVerification.isVerified(json("{\"official_verify\":{\"type\":127,\"desc\":\"\"}}")));
    }

    @Test
    void userCardsUseOfficial() throws Exception {
        assertTrue(BilibiliVerification.isVerified(json("{\"Official\":{\"role\":3,\"title\":\"media\",\"type\":1}}")));
        assertFalse(BilibiliVerification.isVerified(json("{\"Official\":{\"role\":0,\"title\":\"\",\"type\":-1}}")));
    }

    @Test
    void missingVerificationIsNotVerified() throws Exception {
        assertFalse(BilibiliVerification.isVerified(json("{\"uname\":\"fan\"}")));
        assertFalse(BilibiliVerification.isVerified(null));
    }

    private static JsonObject json(final String source) throws Exception {
        return JsonParser.object().from(source);
    }
}
