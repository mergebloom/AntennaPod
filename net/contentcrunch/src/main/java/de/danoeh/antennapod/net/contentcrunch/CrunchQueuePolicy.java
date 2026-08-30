package de.danoeh.antennapod.net.contentcrunch;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Pure filtering and presentation rules for the Crunch Queue. */
public final class CrunchQueuePolicy {
    private CrunchQueuePolicy() { }

    public static List<Long> visibleIds(List<Long> candidateIds, Set<Long> dismissedIds, boolean showDismissed) {
        List<Long> result = new ArrayList<>();
        for (Long id : candidateIds) {
            if (showDismissed || !dismissedIds.contains(id)) { result.add(id); }
        }
        return result;
    }

    public static String state(ContentCrunchModels.EpisodeResult result, boolean processing) {
        if (processing) { return "processing"; }
        if (result == null) { return "not_processed"; }
        if (ContentCrunchPoller.isCompleted(result)) { return "ready"; }
        if (ContentCrunchPoller.isFailed(result)) { return "failed"; }
        return "processing";
    }

    public static String excerpt(String summary, int maxLength) {
        if (summary == null || summary.trim().isEmpty()) { return ""; }
        String normalized = summary.trim().replaceAll("\\s+", " ");
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength - 1) + "…";
    }
}
