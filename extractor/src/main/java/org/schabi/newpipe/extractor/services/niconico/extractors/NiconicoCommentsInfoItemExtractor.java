package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.comments.CommentsInfoItemExtractor;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.Description;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// See https://zenn.dev/negima1072/articles/howto-get-nicovideo-comments
public class NiconicoCommentsInfoItemExtractor implements CommentsInfoItemExtractor {
    public JsonObject json;
    public String url;

    NiconicoCommentsInfoItemExtractor(final JsonObject json, final String url) {
        this.json = json;
        this.url = url;
    }

    @Override
    public String getName() throws ParsingException {
        return null;
    }

    @Override
    public String getUrl() throws ParsingException {
        return url;
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        return null;
    }

    @Override
    public int getLikeCount() throws ParsingException {
        return getScore();
    }

    @Override
    public String getTextualLikeCount() throws ParsingException {
        return String.valueOf(getLikeCount());
    }

    @Override
    public Description getCommentText() throws ParsingException {
        final Duration diff = getDuration();
        @SuppressWarnings("DefaultLocale") final String hms = String.format("%02d:%02d",
                diff.toMinutes(),
                diff.getSeconds() % 60);
        return new Description(hms + " " + getCommentTextRaw(), Description.PLAIN_TEXT);
    }

    @Override
    public String getTextualUploadDate() throws ParsingException {
        return null;
        //new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(getUploadDate().offsetDateTime());
    }

    @Override
    public String getCommentId() throws ParsingException {
        if (json.has("id")) {
            return json.getString("id");
        }
        return json.has("no") ? String.valueOf(json.get("no")) : null;
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        return null;
    }

    @Nullable
    @Override
    public String getUploaderId() throws ParsingException {
        final String userId = json.getString("userId");
        return userId == null || userId.isEmpty() ? null : userId;
    }

    @Override
    public String getUploaderName() throws ParsingException {
        return null; // json.getString("user_id");
    }

    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        return null;
    }

    @Override
    public boolean isHeartedByUploader() throws ParsingException {
        return false;
    }

    @Override
    public boolean isPinned() throws ParsingException {
        return false;
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Nullable
    @Override
    public Page getReplies() throws ParsingException {
        return null;
    }

    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return new DateWrapper(
                OffsetDateTime.ofInstant(
                        json.has("postedAt")
                                ? OffsetDateTime.parse(json.getString("postedAt")).toInstant()
                                : Instant.ofEpochSecond(json.getLong("date")),
                        ZoneOffset.ofHours(0)
                )
        );
    }

    // Additional apis.
    @Nonnull
    private Duration getDuration() {
        if (json.has("vpos")) {
            return Duration.ofMillis(json.getLong("vpos") * 10);
        }
        return Duration.ofMillis(json.getLong("vposMs"));
    }

    private int getScore() {
        return json.has("nicoruCount") ? json.getInt("nicoruCount") : json.getInt("score");
    }

    private String getCommentTextRaw() {
        return json.has("content") ? json.getString("content") : json.getString("body");
    }
}
