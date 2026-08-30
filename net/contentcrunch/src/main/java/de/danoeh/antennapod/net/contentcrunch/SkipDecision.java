package de.danoeh.antennapod.net.contentcrunch;

import java.util.List;
import java.util.Set;

public final class SkipDecision {
    public static final long TOLERANCE_MS = 750;
    private SkipDecision() { }

    public static ContentCrunchModels.SkipSegment find(long positionMs, long lastSkippedEndMs,
            List<ContentCrunchModels.SkipSegment> segments, Set<String> enabledCategories) {
        if (segments == null || enabledCategories == null) {
            return null;
        }
        for (ContentCrunchModels.SkipSegment segment : segments) {
            if (!enabledCategories.contains(segment.category) || segment.endTime <= segment.startTime
                    || Math.abs(segment.endTime - lastSkippedEndMs) <= TOLERANCE_MS) {
                continue;
            }
            if (positionMs >= segment.startTime - TOLERANCE_MS && positionMs < segment.endTime - TOLERANCE_MS) {
                return segment;
            }
        }
        return null;
    }
}
