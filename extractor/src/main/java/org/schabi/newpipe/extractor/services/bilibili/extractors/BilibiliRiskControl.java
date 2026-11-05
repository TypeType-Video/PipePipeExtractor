package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.downloader.Response;

import javax.annotation.Nonnull;

/**
 * Bilibili's risk control blocks a server in two ways: its WAF answers HTTP 412 with an HTML
 * page, and the play URL API answers code 0 with only a {@code v_voucher} captcha challenge and
 * no stream. Both must surface as risk control, not as paid or missing content.
 */
final class BilibiliRiskControl {
    private static final int HTTP_PRECONDITION_FAILED = 412;
    private static final int RISK_CONTROL_CODE = -352;

    private BilibiliRiskControl() {
    }

    static void check(@Nonnull final Response response) throws BilibiliRiskControlException {
        if (response.responseCode() == HTTP_PRECONDITION_FAILED) {
            throw new BilibiliRiskControlException(
                    "Bilibili risk control blocked this server (HTTP 412)", HTTP_PRECONDITION_FAILED);
        }
    }

    static void checkPlayData(@Nonnull final JsonObject data) throws BilibiliRiskControlException {
        final boolean noStream = data.getObject("dash").isEmpty() && data.getArray("durl").isEmpty();
        if (noStream && !data.getString("v_voucher", "").isEmpty()) {
            throw new BilibiliRiskControlException(
                    "Bilibili risk control asked this server for a captcha", RISK_CONTROL_CODE);
        }
    }
}
