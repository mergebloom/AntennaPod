package de.danoeh.antennapod.net.contentcrunch;

import java.util.Collections;
import java.util.List;

public final class ContentCrunchModels {
    private ContentCrunchModels() { }

    public static final class EpisodeKey {
        public final String feedUrl;
        public final String guid;
        public final String audioUrl;

        public EpisodeKey(String feedUrl, String guid, String audioUrl) {
            this.feedUrl = feedUrl;
            this.guid = guid;
            this.audioUrl = audioUrl;
        }
    }

    public static final class SkipSegment {
        public final long startTime;
        public final long endTime;
        public final String category;
        public final double confidence;
        public final String label;

        public SkipSegment(long startTime, long endTime, String category, double confidence, String label) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.category = category;
            this.confidence = confidence;
            this.label = label;
        }
    }

    public static final class EpisodeResult {
        public final String status;
        public final String summary;
        public final List<SkipSegment> skipSegments;

        public EpisodeResult(String status, String summary, List<SkipSegment> skipSegments) {
            this.status = status;
            this.summary = summary;
            this.skipSegments = skipSegments == null ? Collections.emptyList() : skipSegments;
        }
    }
}
