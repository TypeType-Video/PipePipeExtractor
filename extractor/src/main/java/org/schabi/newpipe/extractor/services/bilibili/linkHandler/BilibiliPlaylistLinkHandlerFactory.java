package org.schabi.newpipe.extractor.services.bilibili.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.*;

public class BilibiliPlaylistLinkHandlerFactory extends ListLinkHandlerFactory {
    @Override
    public String getId(String url) throws ParsingException {
        return url;
    }

    @Override
    public boolean onAcceptUrl(String url) throws ParsingException {
        return url != null && (url.contains(GET_SEASON_ARCHIVES_ARCHIVE_BASE_URL) ||
                url.contains(GET_SERIES_BASE_URL) ||
                url.contains(GET_PARTITION_URL) ||
                toApiUrl(url) != null);
    }

    @Override
    public String getUrl(String id, List<FilterItem> contentFilter, List<FilterItem> sortFilter) throws ParsingException {
        return id;
    }

    public static String toApiUrl(final String url) {
        if (url == null) {
            return null;
        }
        final Matcher path = Pattern.compile(
                "^https?://www\\.bilibili\\.com/medialist/play/(\\d+)(?:/)?(?:\\?.*)?$",
                Pattern.CASE_INSENSITIVE).matcher(url.trim());
        if (!path.matches()) {
            return null;
        }
        final String mid = path.group(1);
        final String business = queryParam(url, "business");
        final String businessId = queryParam(url, "business_id");
        if (businessId == null || !businessId.matches("\\d+") || "0".equals(businessId)) {
            return null;
        }
        if ("space_collection".equalsIgnoreCase(business)) {
            return GET_SEASON_ARCHIVES_ARCHIVE_BASE_URL + "?mid=" + mid + "&season_id=" + businessId
                    + "&sort_reverse=false&page_num=1&page_size=30";
        }
        if ("space_series".equalsIgnoreCase(business)) {
            return GET_SERIES_BASE_URL + "?mid=" + mid + "&series_id=" + businessId
                    + "&only_normal=true&sort=desc&pn=1&ps=30";
        }
        return null;
    }

    private static String queryParam(final String url, final String name) {
        final Matcher matcher = Pattern.compile("[?&]" + Pattern.quote(name) + "=([^&#]+)").matcher(url);
        return matcher.find() ? matcher.group(1) : null;
    }
}
