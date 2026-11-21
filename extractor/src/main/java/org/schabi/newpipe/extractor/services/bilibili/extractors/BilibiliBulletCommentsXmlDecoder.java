package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.services.bilibili.utils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.schabi.newpipe.extractor.services.bilibili.extractors.BilibiliStreamMetadataHelper.RISK_CONTROL_CODE;
import static org.schabi.newpipe.extractor.services.bilibili.extractors.BilibiliStreamMetadataHelper.RISK_CONTROL_PRECONDITION;

final class BilibiliBulletCommentsXmlDecoder {
    private BilibiliBulletCommentsXmlDecoder() {
    }

    static Document decode(@Nonnull final Response response) throws ParsingException {
        BilibiliRiskControl.check(response);
        if (response.responseCode() < 200 || response.responseCode() >= 300) {
            throw new ParsingException("Failed to fetch Bilibili bullet comments: HTTP "
                    + response.responseCode());
        }
        byte[] raw = response.rawResponseBody();
        if (raw == null || raw.length == 0) {
            final String bodyText = response.responseBody();
            if (bodyText != null && !bodyText.isEmpty()) {
                raw = bodyText.getBytes(StandardCharsets.UTF_8);
            } else {
                throw new ParsingException("Empty Bilibili bullet comments response");
            }
        }

        final String xml;
        final int firstByte = firstNonWhitespaceByte(raw);
        if (firstByte == '<') {
            xml = new String(raw, StandardCharsets.UTF_8);
        } else if (firstByte == '{' || firstByte == '[') {
            throwUnexpectedPayload(new String(raw, StandardCharsets.UTF_8));
            return null;
        } else {
            byte[] decompressed;
            try {
                decompressed = utils.decompress(raw);
            } catch (IOException rawDeflateError) {
                try {
                    decompressed = utils.decompressZlibStrict(raw);
                } catch (IOException zlibError) {
                    zlibError.addSuppressed(rawDeflateError);
                    throw new ParsingException("Failed to decompress Bilibili bullet comments", zlibError);
                }
            }
            xml = new String(decompressed, StandardCharsets.UTF_8);
        }

        final String normalized = stripBomAndLeadingWhitespace(xml);
        if (normalized.isEmpty()) {
            throw new ParsingException("Empty Bilibili bullet comments XML");
        }
        if (normalized.charAt(0) != '<') {
            throwUnexpectedPayload(normalized);
        }
        return Jsoup.parse(normalized, "", Parser.xmlParser());
    }

    private static int firstNonWhitespaceByte(final byte[] raw) {
        int i = 0;
        if (raw.length >= 3
                && (raw[0] & 0xFF) == 0xEF
                && (raw[1] & 0xFF) == 0xBB
                && (raw[2] & 0xFF) == 0xBF) {
            i = 3;
        }
        while (i < raw.length && (raw[i] & 0xFF) <= 0x20) {
            i++;
        }
        return i < raw.length ? (raw[i] & 0xFF) : -1;
    }

    private static String stripBomAndLeadingWhitespace(final String text) {
        if (text == null) {
            return "";
        }
        int start = 0;
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            start = 1;
        }
        while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
            start++;
        }
        return start == 0 ? text : text.substring(start);
    }

    private static void throwUnexpectedPayload(final String text) throws ParsingException {
        final String trimmed = stripBomAndLeadingWhitespace(text);
        if (trimmed.startsWith("{")) {
            try {
                final JsonObject obj = JsonParser.object().from(trimmed);
                final int code = obj.getInt("code", 0);
                if (code == RISK_CONTROL_CODE || code == RISK_CONTROL_PRECONDITION) {
                    throw new BilibiliRiskControlException(
                            "Bilibili risk control blocked bullet comments", code);
                }
            } catch (JsonParserException ignored) {
            }
        }
        throw new ParsingException("Unexpected non-XML Bilibili bullet comments response");
    }
}
