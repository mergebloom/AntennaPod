package de.danoeh.antennapod.net.contentcrunch;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;

public final class ContentCrunchAutoProcessor {
    private static final String WORK_PREFIX = "content-crunch-auto-process-";

    private ContentCrunchAutoProcessor() { }

    public static boolean isEligible(boolean enabled, String baseUrl, String accessToken, String refreshCookie,
            ContentCrunchModels.EpisodeKey key) {
        return enabled && baseUrl != null && !baseUrl.isEmpty()
                && ((accessToken != null && !accessToken.isEmpty())
                || (refreshCookie != null && !refreshCookie.isEmpty()))
                && EpisodeMatcher.isValid(key);
    }

    public static boolean shouldSubmit(ContentCrunchModels.EpisodeResult result) {
        if (result == null || result.status == null) {
            return true;
        }
        String status = result.status.toUpperCase(Locale.US);
        return status.isEmpty() || "UNKNOWN".equals(status) || "NOT_FOUND".equals(status);
    }

    public static String uniqueWorkName(ContentCrunchModels.EpisodeKey key) {
        String identity = key.feedUrl + "\n" + (key.guid == null || key.guid.isEmpty() ? key.audioUrl : key.guid);
        return WORK_PREFIX + UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8));
    }

    public static void dispatchFailOpen(Runnable dispatch) {
        try {
            dispatch.run();
        } catch (RuntimeException ignored) {
        }
    }
}
