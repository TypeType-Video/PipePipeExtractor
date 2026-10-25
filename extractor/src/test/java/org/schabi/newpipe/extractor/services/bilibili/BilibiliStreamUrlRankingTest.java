package org.schabi.newpipe.extractor.services.bilibili;

import com.grack.nanojson.JsonArray;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BilibiliStreamUrlRankingTest {

    private static final String REGIONAL =
            "https://cn-gddg-ct-01-10.bilivideo.com/upgcxcode/1/2/3/video.m4s?e=1";
    private static final String MIRROR =
            "https://upos-sz-mirrorali.bilivideo.com/upgcxcode/1/2/3/video.m4s?e=1";
    private static final String MCDN =
            "https://xy1x2x3x4xy.mcdn.bilivideo.cn:4483/upgcxcode/1/2/3/video.m4s?e=1";

    @Test
    void prefersAMirrorBackupOverAColdRegionalPrimary() {
        assertEquals(MIRROR, BilibiliService.pickStableStreamUrl(REGIONAL,
                new JsonArray(Arrays.asList(MCDN, MIRROR))));
    }

    @Test
    void keepsTheRegionalPrimaryOverAnMcdnBackup() {
        assertEquals(REGIONAL, BilibiliService.pickStableStreamUrl(REGIONAL,
                new JsonArray(Arrays.asList(MCDN))));
    }

    @Test
    void replacesAnMcdnPrimaryWithAnyOtherHost() {
        assertEquals(REGIONAL, BilibiliService.pickStableStreamUrl(MCDN,
                new JsonArray(Arrays.asList(REGIONAL))));
    }

    @Test
    void keepsTheMcdnPrimaryWhenNothingElseIsOffered() {
        assertEquals(MCDN, BilibiliService.pickStableStreamUrl(MCDN, new JsonArray()));
        assertEquals(MCDN, BilibiliService.pickStableStreamUrl(MCDN, null));
    }
}
