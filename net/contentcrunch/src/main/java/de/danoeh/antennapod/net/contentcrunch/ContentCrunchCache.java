package de.danoeh.antennapod.net.contentcrunch;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ContentCrunchCache {
    private static final Map<String, ContentCrunchModels.EpisodeResult> CACHE = new LinkedHashMap<>();
    private ContentCrunchCache() { }
    private static String key(ContentCrunchModels.EpisodeKey value) {
        return value.feedUrl + "|" + value.guid + "|" + value.audioUrl;
    }
    public static synchronized void put(ContentCrunchModels.EpisodeKey key, ContentCrunchModels.EpisodeResult value) {
        if (!EpisodeMatcher.isValid(key) || !ContentCrunchPoller.isCompleted(value)) { return; }
        CACHE.put(key(key), value);
        while (CACHE.size() > 50) { CACHE.remove(CACHE.keySet().iterator().next()); }
    }
    public static synchronized ContentCrunchModels.EpisodeResult get(ContentCrunchModels.EpisodeKey key) {
        if (!EpisodeMatcher.isValid(key)) { return null; }
        return CACHE.get(key(key));
    }
    public static synchronized void clear() { CACHE.clear(); }
}
