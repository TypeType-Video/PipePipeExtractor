package org.schabi.newpipe.extractor.services.bilibili.extractors;

import org.schabi.newpipe.extractor.exceptions.ParsingException;

public class BilibiliRiskControlException extends ParsingException {
    private final long code;

    public BilibiliRiskControlException(final String message, final long code) {
        super(message);
        this.code = code;
    }

    public long getCode() {
        return code;
    }
}
