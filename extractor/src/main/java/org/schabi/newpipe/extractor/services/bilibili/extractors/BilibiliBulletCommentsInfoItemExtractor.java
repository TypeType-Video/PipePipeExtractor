package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonParser;
import org.jsoup.nodes.Element;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItem;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItemExtractor;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import java.time.Duration;

public class BilibiliBulletCommentsInfoItemExtractor implements BulletCommentsInfoItemExtractor {
    private static final int DEFAULT_ARGB_COLOR = 0xFFFFFFFF;

    private final Element element;
    private final String[] attr;

    public BilibiliBulletCommentsInfoItemExtractor(final Element element) {
        this(element, element.attr("p").split(","));
    }

    BilibiliBulletCommentsInfoItemExtractor(final Element element, final String[] attr) {
        this.element = element;
        this.attr = attr;
    }

    @Override
    public String getCommentText() throws ParsingException {
        final String text = element.text();
        final String mode = attr.length > 1 ? attr[1].trim() : "";
        if (!"7".equals(mode)) {
            return text;
        }
        try {
            final String possibleText = JsonParser.array().from(text).getString(4);
            return possibleText == null || possibleText.isEmpty() ? text : possibleText;
        } catch (Exception e) {
            return text;
        }
    }

    @Override
    public int getArgbColor() throws ParsingException {
        if (attr.length <= 3) {
            return DEFAULT_ARGB_COLOR;
        }
        try {
            final int rgb = Integer.parseInt(attr[3].trim());
            return 0xFF000000 | (rgb & 0x00FFFFFF);
        } catch (NumberFormatException e) {
            return DEFAULT_ARGB_COLOR;
        }
    }

    @Override
    public BulletCommentsInfoItem.Position getPosition() throws ParsingException {
        final String mode = attr.length > 1 ? attr[1].trim() : "";
        if ("4".equals(mode)) {
            return BulletCommentsInfoItem.Position.BOTTOM;
        } else if ("5".equals(mode)) {
            return BulletCommentsInfoItem.Position.TOP;
        }
        return BulletCommentsInfoItem.Position.REGULAR;
    }

    @Override
    public double getRelativeFontSize() throws ParsingException {
        final String size = attr.length > 2 ? attr[2].trim() : "";
        switch (size) {
            case "18":
                return 0.5;
            case "36":
                return 0.7;
            case "25":
            default:
                return 0.6;
        }
    }

    @Override
    public Duration getDuration() throws ParsingException {
        if (attr.length == 0) {
            throw new ParsingException("Missing bullet comment timestamp");
        }
        try {
            final double seconds = Double.parseDouble(attr[0].trim());
            if (!Double.isFinite(seconds) || seconds < 0) {
                throw new ParsingException("Invalid bullet comment timestamp: " + attr[0]);
            }
            return Duration.ofMillis(Math.round(seconds * 1000.0) + 2500L); // 2500 for sync
        } catch (NumberFormatException e) {
            throw new ParsingException("Invalid bullet comment timestamp: " + attr[0], e);
        }
    }
}
