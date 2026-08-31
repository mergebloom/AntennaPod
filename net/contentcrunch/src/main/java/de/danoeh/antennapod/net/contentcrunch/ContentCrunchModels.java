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
        public final String requestId;
        public final String eventsUrl;
        public final String stage;
        public final int progress;
        public final int summarySize;
        public final String summaryStyle;

        public EpisodeResult(String status, String summary, List<SkipSegment> skipSegments) {
            this(status, summary, skipSegments, null, SummaryConfig.DEFAULT_SIZE, SummaryConfig.DEFAULT_STYLE);
        }

        public EpisodeResult(String status, String summary, List<SkipSegment> skipSegments, String requestId,
                int summarySize, String summaryStyle) {
            this(status, summary, skipSegments, requestId, null, null, -1, summarySize, summaryStyle);
        }

        public EpisodeResult(String status, String summary, List<SkipSegment> skipSegments, String requestId,
                String eventsUrl, String stage, int progress, int summarySize, String summaryStyle) {
            this.status = status;
            this.summary = summary == null ? "" : summary;
            this.skipSegments = skipSegments == null ? Collections.emptyList() : skipSegments;
            this.requestId = requestId;
            this.eventsUrl = eventsUrl;
            this.stage = stage;
            this.progress = progress;
            this.summarySize = summarySize;
            this.summaryStyle = summaryStyle;
        }
    }
}
