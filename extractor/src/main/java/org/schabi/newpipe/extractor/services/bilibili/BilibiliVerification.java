package org.schabi.newpipe.extractor.services.bilibili;

import com.grack.nanojson.JsonObject;

public final class BilibiliVerification {
    private static final String[] VERIFICATION_KEYS = {"official_verify", "Official"};
    private static final int PERSONAL = 0;
    private static final int ORGANIZATION = 1;

    private BilibiliVerification() {
    }

    public static boolean isVerified(final JsonObject owner) {
        if (owner == null) {
            return false;
        }
        for (final String key : VERIFICATION_KEYS) {
            final Object verification = owner.get(key);
            if (verification instanceof JsonObject) {
                final int type = ((JsonObject) verification).getInt("type", -1);
                if (type == PERSONAL || type == ORGANIZATION) {
                    return true;
                }
            }
        }
        return false;
    }
}
