package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.downloader.Response;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BilibiliRiskControlTest {
    private static Response response(final int code, final String body) {
        return new Response(code, "", Collections.emptyMap(), body, body.getBytes(), "https://api.bilibili.com");
    }

    @Test
    void wafHtmlAnswerIsABlockedServer() {
        assertThrows(BilibiliRiskControlException.class,
                () -> BilibiliRiskControl.check(response(412, "<!DOCTYPE html><html></html>")));
        assertDoesNotThrow(() -> BilibiliRiskControl.check(response(200, "{\"code\":0}")));
    }

    @Test
    void voucherWithoutStreamIsABlockedServer() throws Exception {
        final JsonObject voucher = JsonParser.object().from("{\"v_voucher\":\"voucher_0123\"}");
        final JsonObject playable = JsonParser.object().from(
                "{\"v_voucher\":\"\",\"dash\":{\"video\":[{\"id\":32}],\"audio\":[]}}");
        final JsonObject paid = JsonParser.object().from("{\"durl\":[]}");

        assertThrows(BilibiliRiskControlException.class, () -> BilibiliRiskControl.checkPlayData(voucher));
        assertDoesNotThrow(() -> BilibiliRiskControl.checkPlayData(playable));
        assertDoesNotThrow(() -> BilibiliRiskControl.checkPlayData(paid));
    }
}
