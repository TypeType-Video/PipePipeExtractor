package org.schabi.newpipe.extractor.stream;

import java.io.Serializable;
import java.util.List;
import org.schabi.newpipe.extractor.Image;

/** Creator-provided end-screen element. Geometry is relative to the video;
 * missing timing and geometry values use -1. The service type is preserved.
 */
public record EndScreenElement(String id, String type, String title, String url,
                               List<Image> thumbnails, long startTimeMs, long endTimeMs,
                               double left, double top, double width, double aspectRatio)
        implements Serializable {
    public EndScreenElement {
        thumbnails = List.copyOf(thumbnails);
    }
}
