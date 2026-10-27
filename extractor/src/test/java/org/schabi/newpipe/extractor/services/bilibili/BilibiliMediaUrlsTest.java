package org.schabi.newpipe.extractor.services.bilibili;

import com.grack.nanojson.JsonArray;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BilibiliMediaUrlsTest {
    private static final String PRIMARY = "https://upos-hz-mirrorakam.akamaized.net/video";
    private static final String BACKUP = "https://upos-sz-mirrorcosov.bilivideo.com/video";
    private static final String THIRD = "https://upos-example.bilivideo.com/video";
    private static final String MCDN = "https://example.mcdn.bilivideo.cn/video";

    @Test
    void keepsDistinctNonMcdnAlternatesWithoutChangingTheSelectedUrl() {
        final BilibiliMediaUrls urls = BilibiliMediaUrls.from(PRIMARY,
                new JsonArray(Arrays.asList(BACKUP, PRIMARY, MCDN, THIRD, BACKUP)));
        assertEquals(PRIMARY, urls.getSelectedUrl());
        assertEquals(Arrays.asList(BACKUP, THIRD), urls.getAlternateUrls());
    }

    @Test
    void retainsThePrimaryAsAnAlternateWhenTheSelectedUrlWasABackup() {
        final String regional = "https://example.bilivideo.com/video";
        final BilibiliMediaUrls urls = BilibiliMediaUrls.from(regional,
                new JsonArray(Arrays.asList(MCDN, BACKUP)));
        assertEquals(BACKUP, urls.getSelectedUrl());
        assertEquals(Collections.singletonList(regional), urls.getAlternateUrls());
    }

    @Test
    void supportsMissingBackups() {
        assertEquals(Collections.emptyList(), BilibiliMediaUrls.from(PRIMARY, null).getAlternateUrls());
    }
}
