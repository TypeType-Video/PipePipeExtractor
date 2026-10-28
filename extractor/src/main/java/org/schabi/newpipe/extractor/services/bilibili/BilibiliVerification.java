package org.schabi.newpipe.extractor.services.bilibili;

import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.channel.ChannelVerification;

public final class BilibiliVerification {
    private static final String[] VERIFICATION_KEYS = {"official_verify", "Official"};
    private static final int PERSONAL = 0;
    private static final int ORGANIZATION = 1;

    private BilibiliVerification() {
    }

    public static boolean isVerified(final JsonObject owner) {
        return verification(owner) != null;
    }

    public static ChannelVerification verification(final JsonObject owner) {
        if (owner == null) {
            return null;
        }
        for (final String key : VERIFICATION_KEYS) {
            final Object value = owner.get(key);
            if (value instanceof JsonObject) {
                final int type = ((JsonObject) value).getInt("type", -1);
                if (type == PERSONAL) {
                    return ChannelVerification.PERSONAL;
                }
                if (type == ORGANIZATION) {
                    return ChannelVerification.ORGANIZATION;
                }
            }
        }
        return null;
    }
}
