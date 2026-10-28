package org.schabi.newpipe.extractor.services.bilibili;

import com.grack.nanojson.JsonObject;

public final class BilibiliVerification {
    private static final String[] VERIFICATION_KEYS = {"official_verify", "Official"};

    private BilibiliVerification() {
    }

    public static boolean isVerified(final JsonObject owner) {
        if (owner == null) {
            return false;
        }
        for (final String key : VERIFICATION_KEYS) {
            final Object verification = owner.get(key);
            if (verification instanceof JsonObject
                    && ((JsonObject) verification).getInt("type", -1) >= 0) {
                return true;
            }
        }
        return false;
    }
}
