package org.schabi.newpipe.extractor.services.bilibili;

import com.grack.nanojson.JsonArray;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class BilibiliMediaUrls implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int MAX_ALTERNATES = 2;
    private final String selectedUrl;
    private final List<String> alternateUrls;

    private BilibiliMediaUrls(final String selectedUrl, final List<String> alternateUrls) {
        this.selectedUrl = selectedUrl;
        this.alternateUrls = Collections.unmodifiableList(new ArrayList<>(alternateUrls));
    }

    public static BilibiliMediaUrls from(final String primary, final JsonArray backups) {
        final String selected = BilibiliService.pickStableStreamUrl(primary, backups);
        final Set<String> candidates = new LinkedHashSet<>();
        candidates.add(primary);
        if (backups != null) {
            for (int i = 0; i < backups.size(); i++) {
                candidates.add(backups.getString(i));
            }
        }
        final List<String> alternates = new ArrayList<>();
        for (final String candidate : candidates) {
            if (candidate != null && !candidate.isEmpty() && !candidate.equals(selected)
                    && !BilibiliService.isMcdnUrl(candidate)) {
                alternates.add(candidate);
                if (alternates.size() == MAX_ALTERNATES) {
                    break;
                }
            }
        }
        return new BilibiliMediaUrls(selected, alternates);
    }

    public String getSelectedUrl() {
        return selectedUrl;
    }

    public List<String> getAlternateUrls() {
        return alternateUrls;
    }
}
